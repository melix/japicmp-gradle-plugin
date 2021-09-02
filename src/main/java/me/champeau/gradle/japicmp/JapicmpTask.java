package me.champeau.gradle.japicmp;

import japicmp.filter.Filter;
import me.champeau.gradle.japicmp.filters.FilterConfiguration;
import me.champeau.gradle.japicmp.report.RichReport;
import me.champeau.gradle.japicmp.report.RuleConfiguration;
import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.JavaVersion;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ResolvedDependency;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.CompileClasspath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.workers.IsolationMode;
import org.gradle.workers.WorkerConfiguration;
import org.gradle.workers.WorkerExecutor;
import org.gradle.util.GradleVersion;

import java.io.File;
import java.net.URISyntaxException;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@CacheableTask
public class JapicmpTask extends DefaultTask {

    private List<String> packageIncludes = new ArrayList<>();
    private List<String> packageExcludes = new ArrayList<>();
    private List<String> classIncludes = new ArrayList<>();
    private List<String> classExcludes = new ArrayList<>();
    private List<String> methodIncludes = new ArrayList<>();
    private List<String> methodExcludes = new ArrayList<>();
    private List<String> fieldIncludes = new ArrayList<>();
    private List<String> fieldExcludes = new ArrayList<>();
    private List<String> annotationIncludes = new ArrayList<>();
    private List<String> annotationExcludes = new ArrayList<>();
    private List<FilterConfiguration> includeFilters = new ArrayList<>();
    private List<FilterConfiguration> excludeFilters = new ArrayList<>();
    private String accessModifier = "public";
    private boolean onlyModified = false;
    private boolean onlyBinaryIncompatibleModified = false;
    private boolean failOnSourceIncompatibility = false;
    private File xmlOutputFile;
    private File htmlOutputFile;
    private File txtOutputFile;
    private boolean failOnModification = false;
    private boolean includeSynthetic = false;
    private FileCollection oldClasspath;
    private FileCollection newClasspath;
    private FileCollection oldArchives;
    private FileCollection newArchives;
    private boolean ignoreMissingClasses = false;
    private RichReport richReport;

    @TaskAction
    public void exec() {
        WorkerExecutor workerExecutor = getServices().get(WorkerExecutor.class);
        workerExecutor.submit(JApiCmpWorkerAction.class, new Action<WorkerConfiguration>() {
            @Override
            public void execute(final WorkerConfiguration workerConfiguration) {
                workerConfiguration.setIsolationMode(IsolationMode.PROCESS);
                Set<File> classpath = new HashSet<>();
                if (includeFilters != null) {
                    for (FilterConfiguration configuration : includeFilters) {
                        addClasspathFor(configuration.getFilterClass(), classpath);
                    }
                }
                if (excludeFilters != null) {
                    for (FilterConfiguration configuration : excludeFilters) {
                        addClasspathFor(configuration.getFilterClass(), classpath);
                    }
                }
                if (richReport != null) {
                    for (RuleConfiguration configuration : richReport.getRules()) {
                        addClasspathFor(configuration.getRuleClass(), classpath);
                    }
                }
                if (JavaVersion.current().isJava9Compatible()) {
                    classpath.addAll(resolveJaxb().getFiles());
                }
                if (GradleVersion.current().compareTo(GradleVersion.version("6.0")) >= 0) {
                    classpath.addAll(resolveGuava().getFiles());
                }
                workerConfiguration.setClasspath(classpath);
                List<JApiCmpWorkerAction.Archive> baseline = JapicmpTask.this.oldArchives != null ? toArchives(JapicmpTask.this.oldArchives) : inferArchives(oldClasspath);
                List<JApiCmpWorkerAction.Archive> current = JapicmpTask.this.newArchives != null ? toArchives(JapicmpTask.this.newArchives) : inferArchives(newClasspath);
                workerConfiguration.setDisplayName("JApicmp check comparing " + current + " with " + baseline);
                workerConfiguration.params(
                        // we use a single configuration object, instead of passing each parameter directly,
                        // because the worker API doesn't support "null" values
                        new JapiCmpWorkerConfiguration(
                                isIncludeSynthetic(),
                                isIgnoreMissingClasses(),
                                getPackageIncludes(),
                                getPackageExcludes(),
                                getClassIncludes(),
                                getClassExcludes(),
                                getMethodIncludes(),
                                getMethodExcludes(),
                                getFieldIncludes(),
                                getFieldExcludes(),
                                getAnnotationIncludes(),
                                getAnnotationExcludes(),
                                getIncludeFilters(),
                                getExcludeFilters(),
                                toArchives(getOldClasspath()),
                                toArchives(getNewClasspath()),
                                baseline,
                                current,
                                isOnlyModified(),
                                isOnlyBinaryIncompatibleModified(),
                                isFailOnSourceIncompatibility(),
                                getAccessModifier(),
                                getXmlOutputFile(),
                                getHtmlOutputFile(),
                                getTxtOutputFile(),
                                isFailOnModification(),
                                getProject().getBuildDir(),
                                richReport
                        )
                );
            }
        });

    }

    private Configuration resolveJaxb() {
        Project project = getProject();
        DependencyHandler dependencies = project.getDependencies();
        return project.getConfigurations().detachedConfiguration(
                dependencies.create("javax.xml.bind:jaxb-api:2.3.0"),
                dependencies.create("com.sun.xml.bind:jaxb-core:2.3.0.1"),
                dependencies.create("com.sun.xml.bind:jaxb-impl:2.3.0.1"),
                dependencies.create("javax.activation:activation:1.1.1")
        );
    }

    private Configuration resolveGuava() {
        Project project = getProject();
        DependencyHandler dependencies = project.getDependencies();
        return project.getConfigurations().detachedConfiguration(
                dependencies.create("com.google.guava:guava:30.1.1-jre")
        );
    }

    private void addClasspathFor(Class<?> clazz, Set<File> classpath) {
        ProtectionDomain domain = clazz.getProtectionDomain();
        CodeSource codeSource = domain.getCodeSource();
        if (codeSource != null) {
            try {
                classpath.add(new File(codeSource.getLocation().toURI()));
            } catch (URISyntaxException e) {
                // silent
            }
        }
    }

    private List<JApiCmpWorkerAction.Archive> inferArchives(FileCollection fc) {
        if (fc instanceof Configuration) {
            final List<JApiCmpWorkerAction.Archive> archives = new ArrayList<>();
            Set<ResolvedDependency> firstLevelModuleDependencies = ((Configuration) fc).getResolvedConfiguration().getFirstLevelModuleDependencies();
            for (ResolvedDependency moduleDependency : firstLevelModuleDependencies) {
                collectArchives(archives, moduleDependency);
            }
            return archives;
        }

        return toArchives(fc);
    }

    private static List<JApiCmpWorkerAction.Archive> toArchives(FileCollection fc) {
        Set<File> files = fc.getFiles();
        List<JApiCmpWorkerAction.Archive> archives = new ArrayList<>(files.size());
        for (File file : files) {
            archives.add(new JApiCmpWorkerAction.Archive(file, "1.0"));
        }
        return archives;
    }

    private void collectArchives(final List<JApiCmpWorkerAction.Archive> archives, ResolvedDependency resolvedDependency) {
        String version = resolvedDependency.getModule().getId().getVersion();
        archives.add(new JApiCmpWorkerAction.Archive(resolvedDependency.getAllModuleArtifacts().iterator().next().getFile(), version));
        for (ResolvedDependency dependency : resolvedDependency.getChildren()) {
            collectArchives(archives, dependency);
        }
    }

    public void richReport(Action<? super RichReport> configureAction) {
        if (richReport == null) {
            richReport = new RichReport();
        }

        configureAction.execute(richReport);
    }

    @Input
    @Optional
    public List<String> getPackageIncludes() {
        return packageIncludes;
    }

    public void setPackageIncludes(List<String> packageIncludes) {
        this.packageIncludes = packageIncludes;
    }

    @Input
    @Optional
    public List<String> getPackageExcludes() {
        return packageExcludes;
    }

    public void setPackageExcludes(List<String> packageExcludes) {
        this.packageExcludes = packageExcludes;
    }

    @Input
    @Optional
    public List<String> getClassIncludes() {
        return classIncludes;
    }

    public void setClassIncludes(List<String> classIncludes) {
        this.classIncludes = classIncludes;
    }

    @Input
    @Optional
    public List<String> getClassExcludes() {
        return classExcludes;
    }

    public void setClassExcludes(List<String> classExcludes) {
        this.classExcludes = classExcludes;
    }

    @Input
    @Optional
    public List<String> getMethodIncludes() {
        return methodIncludes;
    }

    public void setMethodIncludes(List<String> methodIncludes) {
        this.methodIncludes = methodIncludes;
    }

    @Input
    @Optional
    public List<String> getMethodExcludes() {
        return methodExcludes;
    }

    public void setMethodExcludes(List<String> methodExcludes) {
        this.methodExcludes = methodExcludes;
    }

    @Input
    @Optional
    public List<String> getFieldIncludes() {
        return fieldIncludes;
    }

    public void setFieldIncludes(List<String> fieldIncludes) {
        this.fieldIncludes = fieldIncludes;
    }

    @Input
    @Optional
    public List<String> getFieldExcludes() {
        return fieldExcludes;
    }

    public void setFieldExcludes(List<String> fieldExcludes) {
        this.fieldExcludes = fieldExcludes;
    }

    @Input
    @Optional
    public List<String> getAnnotationIncludes() {
        return annotationIncludes;
    }

    public void setAnnotationIncludes(List<String> annotationIncludes) {
        this.annotationIncludes = annotationIncludes;
    }

    @Input
    @Optional
    public List<String> getAnnotationExcludes() {
        return annotationExcludes;
    }

    public void setAnnotationExcludes(List<String> annotationExcludes) {
        this.annotationExcludes = annotationExcludes;
    }

    @Input
    @Optional
    public List<FilterConfiguration> getIncludeFilters() {
        return includeFilters;
    }

    public void setIncludeFilters(List<FilterConfiguration> includeFilters) {
        this.includeFilters = includeFilters;
    }

    public void addIncludeFilter(Class<? extends Filter> includeFilterClass) {
        includeFilters.add(new FilterConfiguration(includeFilterClass));
    }

    @Input
    @Optional
    public List<FilterConfiguration> getExcludeFilters() {
        return excludeFilters;
    }

    public void setExcludeFilters(List<FilterConfiguration> excludeFilters) {
        this.excludeFilters = excludeFilters;
    }

    public void addExcludeFilter(Class<? extends Filter> excludeFilterClass) {
        excludeFilters.add(new FilterConfiguration(excludeFilterClass));
    }

    @Input
    @Optional
    public String getAccessModifier() {
        return accessModifier;
    }

    public void setAccessModifier(String accessModifier) {
        this.accessModifier = accessModifier;
    }

    @Input
    public boolean isOnlyModified() {
        return onlyModified;
    }

    public void setOnlyModified(boolean onlyModified) {
        this.onlyModified = onlyModified;
    }

    @Input
    public boolean isOnlyBinaryIncompatibleModified() {
        return onlyBinaryIncompatibleModified;
    }

    public void setOnlyBinaryIncompatibleModified(boolean onlyBinaryIncompatibleModified) {
        this.onlyBinaryIncompatibleModified = onlyBinaryIncompatibleModified;
    }

    @Input
    public boolean isFailOnSourceIncompatibility() {
        return failOnSourceIncompatibility;
    }

    public void setFailOnSourceIncompatibility(boolean failOnSourceIncompatibility) {
        this.failOnSourceIncompatibility = failOnSourceIncompatibility;
    }

    @OutputFile
    @Optional
    public File getXmlOutputFile() {
        return xmlOutputFile;
    }

    public void setXmlOutputFile(File xmlOutputFile) {
        this.xmlOutputFile = xmlOutputFile;
    }

    @OutputFile
    @Optional
    public File getHtmlOutputFile() {
        return htmlOutputFile;
    }

    public void setHtmlOutputFile(File htmlOutputFile) {
        this.htmlOutputFile = htmlOutputFile;
    }

    @OutputFile
    @Optional
    public File getTxtOutputFile() {
        return txtOutputFile;
    }

    public void setTxtOutputFile(File txtOutputFile) {
        this.txtOutputFile = txtOutputFile;
    }

    @Input
    public boolean isFailOnModification() {
        return failOnModification;
    }

    public void setFailOnModification(boolean failOnModification) {
        this.failOnModification = failOnModification;
    }

    @Input
    public boolean isIncludeSynthetic() {
        return includeSynthetic;
    }

    public void setIncludeSynthetic(boolean includeSynthetic) {
        this.includeSynthetic = includeSynthetic;
    }

    @CompileClasspath
    public FileCollection getOldClasspath() {
        return oldClasspath;
    }

    public void setOldClasspath(FileCollection oldClasspath) {
        this.oldClasspath = oldClasspath;
    }

    @CompileClasspath
    public FileCollection getNewClasspath() {
        return newClasspath;
    }

    public void setNewClasspath(FileCollection newClasspath) {
        this.newClasspath = newClasspath;
    }

    @Optional
    @CompileClasspath
    public FileCollection getOldArchives() {
        return oldArchives;
    }

    public void setOldArchives(FileCollection oldArchives) {
        this.oldArchives = oldArchives;
    }

    @Optional
    @CompileClasspath
    public FileCollection getNewArchives() {
        return newArchives;
    }

    public void setNewArchives(FileCollection newArchives) {
        this.newArchives = newArchives;
    }

    @Input
    public boolean isIgnoreMissingClasses() {
        return ignoreMissingClasses;
    }

    public void setIgnoreMissingClasses(boolean ignoreMissingClasses) {
        this.ignoreMissingClasses = ignoreMissingClasses;
    }

    @Optional
    @Nested
    public RichReport getRichReport() {
        return richReport;
    }

    public void setRichReport(RichReport richReport) {
        this.richReport = richReport;
    }

}
