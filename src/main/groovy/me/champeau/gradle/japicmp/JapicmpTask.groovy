package me.champeau.gradle.japicmp

import groovy.transform.CompileStatic
import japicmp.cmp.JApiCmpArchive
import japicmp.cmp.JarArchiveComparator
import japicmp.cmp.JarArchiveComparatorOptions
import japicmp.config.Options
import japicmp.filter.JavadocLikePackageFilter
import japicmp.model.AccessModifier
import japicmp.model.JApiClass
import japicmp.output.stdout.StdoutOutputGenerator
import japicmp.output.xml.XmlOutput
import japicmp.output.xml.XmlOutputGenerator
import japicmp.output.xml.XmlOutputGeneratorOptions
import me.champeau.gradle.japicmp.report.RichReport
import me.champeau.gradle.japicmp.report.RichReportData
import me.champeau.gradle.japicmp.report.Severity
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.*

import java.util.regex.Pattern

@CompileStatic
class JapicmpTask extends DefaultTask {
    private final static Closure<Boolean> DEFAULT_BREAK_BUILD_CHECK = { JApiClass it -> !it.binaryCompatible }

    @Input
    @Optional
    List<String> packageIncludes = []

    @Input
    @Optional
    List<String> packageExcludes = []

    @Input
    @Optional
    String accessModifier = 'public'

    @Input
    @Optional
    boolean onlyModified = false

    @Input
    @Optional
    boolean onlyBinaryIncompatibleModified = false

    @OutputFile
    @Optional
    File xmlOutputFile

    @OutputFile
    @Optional
    File htmlOutputFile

    @OutputFile
    @Optional File txtOutputFile

    @Input
    @Optional
    boolean failOnModification = false

    @Input
    @Optional
    boolean includeSynthetic = false

    @Input
    @CompileClasspath
    FileCollection oldClasspath

    @Input
    @CompileClasspath
    FileCollection newClasspath

    @Optional
    @Input
    boolean ignoreMissingClasses = false

    @Optional
    @Input
    @Nested
    RichReport richReport

    @TaskAction
    void exec() {
        def comparatorOptions = createOptions()
        def jarArchiveComparator = new JarArchiveComparator(comparatorOptions)
        generateOutput(jarArchiveComparator)
    }

    private JarArchiveComparatorOptions createOptions() {
        def options = new JarArchiveComparatorOptions()
        options.classPathMode = JarArchiveComparatorOptions.ClassPathMode.TWO_SEPARATE_CLASSPATHS
        options.includeSynthetic = includeSynthetic
        options.ignoreMissingClasses.setIgnoreAllMissingClasses(ignoreMissingClasses)
        options.with {
            filters.getIncludes().addAll(packageIncludes.collect { new JavadocLikePackageFilter(it) })
            filters.getExcludes().addAll(packageExcludes.collect { new JavadocLikePackageFilter(it) })
        }
        options
    }

    private List<JApiCmpArchive> toArchives(FileCollection fc) {
        List<JApiCmpArchive> archives = []
        if (fc instanceof Configuration) {
            fc.resolvedConfiguration.firstLevelModuleDependencies.each {
                collectArchives(archives, it)
            }
        } else {
            fc.files.collect(archives) {
                new JApiCmpArchive(it, '1.0')
            }
        }
        archives
    }

    void collectArchives(List<JApiCmpArchive> archives, ResolvedDependency resolvedDependency) {
        archives << new JApiCmpArchive(resolvedDependency.allModuleArtifacts.first().file, resolvedDependency.module.id.version)
        resolvedDependency.children.each {
            collectArchives(archives, it)
        }
    }


    private void generateOutput(JarArchiveComparator jarArchiveComparator) {
        // we create a dummy options because we don't want to avoid use of internal classes of JApicmp
        def options = Options.newDefault()
        options.oldClassPath = com.google.common.base.Optional.of(oldClasspath.asPath)
        options.newClassPath = com.google.common.base.Optional.of(newClasspath.asPath)
        def baseline = toArchives(oldClasspath)
        def current = toArchives(newClasspath)
        println "Comparing ${current} to ${baseline}"
        List<JApiClass> jApiClasses = jarArchiveComparator.compare(baseline, current)
        options.outputOnlyModifications = onlyModified
        options.outputOnlyBinaryIncompatibleModifications = onlyBinaryIncompatibleModified
        options.includeSynthetic = includeSynthetic
        options.setAccessModifier(AccessModifier.valueOf(accessModifier.toUpperCase()))
        if (xmlOutputFile) {
            options.xmlOutputFile = com.google.common.base.Optional.of(xmlOutputFile.getAbsolutePath())
        }
        if (htmlOutputFile) {
            options.htmlOutputFile = com.google.common.base.Optional.of(htmlOutputFile.getAbsolutePath())
        }
        if (xmlOutputFile || htmlOutputFile) {
            def xmlOptions = new XmlOutputGeneratorOptions()
            def xmlOutputGenerator = new XmlOutputGenerator(jApiClasses, options, xmlOptions)
            XmlOutput xmlOutput = xmlOutputGenerator.generate()
            XmlOutputGenerator.writeToFiles(options, xmlOutput)
        }
        if (txtOutputFile) {
            StdoutOutputGenerator stdoutOutputGenerator = new StdoutOutputGenerator(options, jApiClasses)
            String output = stdoutOutputGenerator.generate()
            txtOutputFile.write(output)
        }

        boolean hasCustomViolations = false
        if (richReport) {
            if (richReport.includedClasses) {
                richReport.violationsGenerator.classFilter = { String className ->
                    richReport.includedClasses.any {
                        Pattern.compile(it).matcher(className).find() && (
                                !richReport.excludedClasses || !richReport.excludedClasses.any { Pattern.compile(it).matcher(className).find() })
                    }
                }
            }
            def violations = richReport.violationsGenerator.toViolations(jApiClasses)
            hasCustomViolations = violations.values().any {
                it.any { it.severity == Severity.error }
            }
            def path = richReport.destinationDir
            if (path==null) {
                path = project.file("$project.buildDir/reports/")
            }
            richReport.renderer.render(new File(path, richReport.reportName), new RichReportData(richReport.title, richReport.description, violations))
        }


        if (failOnModification && (hasCustomViolations || jApiClasses.any(DEFAULT_BREAK_BUILD_CHECK))) {
            throw new GradleException("Detected binary changes between ${baseline*.file.name} and ${current*.file.name}")
        }

        jApiClasses
    }

    void richReport(Action<? super RichReport> configureAction) {
        if (richReport == null) {
            richReport = new RichReport(this)
        }
        configureAction.execute(richReport)
    }

}
