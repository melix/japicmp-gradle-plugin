package me.champeau.gradle.japicmp;

import me.champeau.gradle.japicmp.report.RichReport;
import me.champeau.gradle.japicmp.report.ViolationRuleConfiguration;
import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ResolvedDependency;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.CompileClasspath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.workers.IsolationMode;
import org.gradle.workers.WorkerConfiguration;
import org.gradle.workers.WorkerExecutor;

import java.io.File;
import java.net.URISyntaxException;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JapicmpTask extends DefaultTask {

    private List<String> packageIncludes = new ArrayList<>();
    private List<String> packageExcludes = new ArrayList<>();
    private String accessModifier = "public";
    private boolean onlyModified = false;
    private boolean onlyBinaryIncompatibleModified = false;
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
                workerConfiguration.setIsolationMode(IsolationMode.CLASSLOADER);
                Set<File> classpath = new HashSet<>();
                for (ViolationRuleConfiguration configuration : richReport.getRules()) {
                    ProtectionDomain domain = configuration.getRuleClass().getProtectionDomain();
                    CodeSource codeSource = domain.getCodeSource();
                    if (codeSource != null) {
                        try {
                            classpath.add(new File(codeSource.getLocation().toURI()));
                        } catch (URISyntaxException e) {
                            // silent
                        }
                    }
                }
                workerConfiguration.setClasspath(classpath);
                List<JApiCmpWorkerAction.Archive> baseline = JapicmpTask.this.oldArchives != null ? toArchives(JapicmpTask.this.oldArchives) : inferArchives(oldClasspath);
                List<JApiCmpWorkerAction.Archive> current = JapicmpTask.this.newArchives != null ? toArchives(JapicmpTask.this.newArchives) : inferArchives(newClasspath);
                workerConfiguration.setDisplayName("Comparing " + current + " with " + baseline);
                workerConfiguration.params(
                        // we use a single configuration object, instead of passing each parameter directly,
                        // because the worker API doesn't support "null" values
                        new JapiCmpWorkerConfiguration(
                                getIncludeSynthetic(),
                                getIgnoreMissingClasses(),
                                getPackageIncludes(),
                                getPackageExcludes(),
                                toArchives(getOldClasspath()),
                                toArchives(getNewClasspath()),
                                baseline,
                                current,
                                getOnlyModified(),
                                getOnlyBinaryIncompatibleModified(),
                                getAccessModifier(),
                                getXmlOutputFile(),
                                getHtmlOutputFile(),
                                getTxtOutputFile(),
                                getFailOnModification(),
                                getProject().getBuildDir(),
                                richReport
                        )
                );
            }
        });

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
    public String getAccessModifier() {
        return accessModifier;
    }

    public void setAccessModifier(String accessModifier) {
        this.accessModifier = accessModifier;
    }

    @Input
    @Optional
    public boolean getOnlyModified() {
        return onlyModified;
    }

    public boolean isOnlyModified() {
        return onlyModified;
    }

    public void setOnlyModified(boolean onlyModified) {
        this.onlyModified = onlyModified;
    }

    @Input
    @Optional
    public boolean getOnlyBinaryIncompatibleModified() {
        return onlyBinaryIncompatibleModified;
    }

    public boolean isOnlyBinaryIncompatibleModified() {
        return onlyBinaryIncompatibleModified;
    }

    public void setOnlyBinaryIncompatibleModified(boolean onlyBinaryIncompatibleModified) {
        this.onlyBinaryIncompatibleModified = onlyBinaryIncompatibleModified;
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
    @Optional
    public boolean getFailOnModification() {
        return failOnModification;
    }

    public boolean isFailOnModification() {
        return failOnModification;
    }

    public void setFailOnModification(boolean failOnModification) {
        this.failOnModification = failOnModification;
    }

    @Input
    @Optional
    public boolean getIncludeSynthetic() {
        return includeSynthetic;
    }

    public boolean isIncludeSynthetic() {
        return includeSynthetic;
    }

    public void setIncludeSynthetic(boolean includeSynthetic) {
        this.includeSynthetic = includeSynthetic;
    }

    @Input
    @CompileClasspath
    public FileCollection getOldClasspath() {
        return oldClasspath;
    }

    public void setOldClasspath(FileCollection oldClasspath) {
        this.oldClasspath = oldClasspath;
    }

    @Input
    @CompileClasspath
    public FileCollection getNewClasspath() {
        return newClasspath;
    }

    public void setNewClasspath(FileCollection newClasspath) {
        this.newClasspath = newClasspath;
    }

    @Input
    @Optional
    @CompileClasspath
    public FileCollection getOldArchives() {
        return oldArchives;
    }

    public void setOldArchives(FileCollection oldArchives) {
        this.oldArchives = oldArchives;
    }

    @Input
    @Optional
    @CompileClasspath
    public FileCollection getNewArchives() {
        return newArchives;
    }

    public void setNewArchives(FileCollection newArchives) {
        this.newArchives = newArchives;
    }

    @Optional
    @Input
    public boolean getIgnoreMissingClasses() {
        return ignoreMissingClasses;
    }

    public boolean isIgnoreMissingClasses() {
        return ignoreMissingClasses;
    }

    public void setIgnoreMissingClasses(boolean ignoreMissingClasses) {
        this.ignoreMissingClasses = ignoreMissingClasses;
    }

    @Optional
    @Input
    @Nested
    public RichReport getRichReport() {
        return richReport;
    }

    public void setRichReport(RichReport richReport) {
        this.richReport = richReport;
    }

}
