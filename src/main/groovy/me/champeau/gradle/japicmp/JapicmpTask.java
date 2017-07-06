package me.champeau.gradle.japicmp;

import japicmp.cmp.JApiCmpArchive;
import japicmp.cmp.JarArchiveComparator;
import japicmp.cmp.JarArchiveComparatorOptions;
import japicmp.config.Options;
import japicmp.filter.JavadocLikePackageFilter;
import japicmp.model.AccessModifier;
import japicmp.model.JApiClass;
import japicmp.output.stdout.StdoutOutputGenerator;
import japicmp.output.xml.XmlOutput;
import japicmp.output.xml.XmlOutputGenerator;
import japicmp.output.xml.XmlOutputGeneratorOptions;
import me.champeau.gradle.japicmp.report.RichReport;
import me.champeau.gradle.japicmp.report.RichReportData;
import me.champeau.gradle.japicmp.report.Severity;
import me.champeau.gradle.japicmp.report.Violation;
import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.Transformer;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ResolvedDependency;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.CompileClasspath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

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
        JarArchiveComparatorOptions comparatorOptions = createOptions();
        JarArchiveComparator jarArchiveComparator = new JarArchiveComparator(comparatorOptions);
        generateOutput(jarArchiveComparator);
    }

    private JarArchiveComparatorOptions createOptions() {
        JarArchiveComparatorOptions options = new JarArchiveComparatorOptions();
        options.setClassPathMode(JarArchiveComparatorOptions.ClassPathMode.TWO_SEPARATE_CLASSPATHS);
        options.setIncludeSynthetic(getIncludeSynthetic());
        options.getIgnoreMissingClasses().setIgnoreAllMissingClasses(getIgnoreMissingClasses());
        for (String packageInclude : getPackageIncludes()) {
            options.getFilters().getIncludes().add(new JavadocLikePackageFilter(packageInclude));
        }
        for (String packageExclude : getPackageExcludes()) {
            options.getFilters().getExcludes().add(new JavadocLikePackageFilter(packageExclude));
        }
        return options;
    }

    private List<JApiCmpArchive> inferArchives(FileCollection fc) {
        if (fc instanceof Configuration) {
            final List<JApiCmpArchive> archives = new ArrayList<>();
            Set<ResolvedDependency> firstLevelModuleDependencies = ((Configuration) fc).getResolvedConfiguration().getFirstLevelModuleDependencies();
            for (ResolvedDependency moduleDependency : firstLevelModuleDependencies) {
                collectArchives(archives, moduleDependency);
            }
            return archives;
        }

        return toJApiCmpArchives(fc);
    }

    private static List<JApiCmpArchive> toJApiCmpArchives(FileCollection fc) {
        Set<File> files = fc.getFiles();
        List<JApiCmpArchive> archives = new ArrayList<>(files.size());
        for (File file : files) {
            archives.add(new JApiCmpArchive(file, "1.0"));
        }
        return archives;
    }

    private void collectArchives(final List<JApiCmpArchive> archives, ResolvedDependency resolvedDependency) {
        String version = resolvedDependency.getModule().getId().getVersion();
        archives.add(new JApiCmpArchive(resolvedDependency.getAllModuleArtifacts().iterator().next().getFile(), version));
        for (ResolvedDependency dependency : resolvedDependency.getChildren()) {
            collectArchives(archives, dependency);
        }
    }

    private static String prettyPrint(List<JApiCmpArchive> archives) {
        StringBuilder sb = new StringBuilder();
        for (JApiCmpArchive archive : archives) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(archive.getFile().getName());
        }
        return sb.toString();
    }

    private static boolean matches(String pattern, String className) {
        return Pattern.compile(pattern).matcher(className).find();
    }

    private void generateOutput(JarArchiveComparator jarArchiveComparator) {
        // we create a dummy options because we don't want to avoid use of internal classes of JApicmp
        Options options = Options.newDefault();
        options.setOldClassPath(com.google.common.base.Optional.of(oldClasspath.getAsPath()));
        options.setNewClassPath(com.google.common.base.Optional.of(newClasspath.getAsPath()));
        final List<JApiCmpArchive> baseline = oldArchives!=null ? toJApiCmpArchives(oldArchives) : inferArchives(oldClasspath);
        final List<JApiCmpArchive> current = newArchives!=null ? toJApiCmpArchives(newArchives) : inferArchives(newClasspath);
        System.out.println("Comparing " + prettyPrint(current) + " to " + prettyPrint(baseline));
        List<JApiClass> jApiClasses = jarArchiveComparator.compare(baseline, current);
        options.setOutputOnlyModifications(onlyModified);
        options.setOutputOnlyBinaryIncompatibleModifications(onlyBinaryIncompatibleModified);
        options.setIncludeSynthetic(includeSynthetic);
        options.setAccessModifier(AccessModifier.valueOf(accessModifier.toUpperCase()));
        if (xmlOutputFile != null) {
            options.setXmlOutputFile(com.google.common.base.Optional.of(xmlOutputFile.getAbsolutePath()));
        }

        if (htmlOutputFile != null) {
            options.setHtmlOutputFile(com.google.common.base.Optional.of(htmlOutputFile.getAbsolutePath()));
        }

        if (xmlOutputFile != null || htmlOutputFile != null) {
            XmlOutputGeneratorOptions xmlOptions = new XmlOutputGeneratorOptions();
            XmlOutputGenerator xmlOutputGenerator = new XmlOutputGenerator(jApiClasses, options, xmlOptions);
            XmlOutput xmlOutput = xmlOutputGenerator.generate();
            XmlOutputGenerator.writeToFiles(options, xmlOutput);
        }

        if (txtOutputFile != null) {
            StdoutOutputGenerator stdoutOutputGenerator = new StdoutOutputGenerator(options, jApiClasses);
            String output = stdoutOutputGenerator.generate();
            try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(txtOutputFile), "utf-8")
            )) {
                writer.write(output);
            } catch (IOException ex) {
                throw new GradleException("Unable to write report", ex);
            }
        }

        boolean hasCustomViolations = false;
        if (richReport != null) {
            final List<String> includedClasses = richReport.getIncludedClasses();
            final List<String> excludedClasses = richReport.getExcludedClasses();
            if (includedClasses != null) {
                richReport.getViolationsGenerator().setClassFilter(
                        new Transformer<Boolean, String>() {
                            @Override
                            public Boolean transform(final String className) {
                                for (String pattern : includedClasses) {
                                    if (matches(pattern, className)) {
                                        if (excludedClasses != null) {
                                            for (String excludePattern : excludedClasses) {
                                                if (matches(excludePattern, className)) {
                                                    return false;
                                                }
                                            }
                                        }
                                        return true;
                                    }
                                }
                                return false;
                            }
                        });
            }

            Map<String, List<Violation>> violations = richReport.getViolationsGenerator().toViolations(jApiClasses);
            for (List<Violation> violationList : violations.values()) {
                for (Violation violation : violationList) {
                    if (violation.getSeverity().equals(Severity.error)) {
                        hasCustomViolations = true;
                        break;
                    }
                }
                if (hasCustomViolations) {
                    break;
                }
            }
            File path = richReport.getDestinationDir();
            if (path == null) {
                path = getProject().file(getProject().getBuildDir() + "/reports/");
            }

            richReport.getRenderer().render(new File(path, richReport.getReportName()), new RichReportData(richReport.getTitle(), richReport.getDescription(), violations));
        }


        if (failOnModification && (hasCustomViolations || hasBreakingChange(jApiClasses))) {
            throw new GradleException("Detected binary changes between " + prettyPrint(current) + " and " + prettyPrint(baseline));
        }
    }

    private static boolean hasBreakingChange(final List<JApiClass> jApiClasses) {
        for (JApiClass jApiClass : jApiClasses) {
            if (!jApiClass.isBinaryCompatible()) {
                return true;
            }
        }
        return false;
    }

    public void richReport(Action<? super RichReport> configureAction) {
        if (richReport == null) {
            richReport = new RichReport(this);
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
