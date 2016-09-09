package me.champeau.gradle

import japicmp.cmp.JarArchiveComparator
import japicmp.cmp.JarArchiveComparatorOptions
import japicmp.config.Options
import japicmp.filter.JavadocLikePackageFilter
import japicmp.model.AccessModifier
import japicmp.model.JApiChangeStatus
import japicmp.model.JApiClass
import japicmp.output.stdout.StdoutOutputGenerator
import japicmp.output.xml.XmlOutput
import japicmp.output.xml.XmlOutputGenerator
import japicmp.output.xml.XmlOutputGeneratorOptions
import org.gradle.api.GradleException
import org.gradle.api.internal.AbstractTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.Optional

abstract class JapicmpAbstractTask extends AbstractTask {
    private final static Closure<Boolean> DEFAULT_BREAK_BUILD_CHECK = { it.changeStatus != JApiChangeStatus.UNCHANGED }

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
    Collection<File> classpath = null

    @Input
    @Optional
    boolean includeSynthetic = false

    private final OutputProcessorBuilder builder = new OutputProcessorBuilder(this)

    abstract File getOldArchive()

    abstract File getNewArchive()

    @TaskAction
    void exec() {
        def comparatorOptions = createOptions()
        def jarArchiveComparator = new JarArchiveComparator(comparatorOptions)
        def jApiClasses = jarArchiveComparator.compare(getOldArchive(), getNewArchive())
        generateOutput(jApiClasses)
        breakBuildIfNecessary(jApiClasses)
    }

    private void breakBuildIfNecessary(final List<JApiClass> jApiClasses) {
        // todo: provide custom checks
        if (failOnModification && jApiClasses.any(DEFAULT_BREAK_BUILD_CHECK)) {
            throw new GradleException("${newArchive} contains binary changes")
        }
    }

    private JarArchiveComparatorOptions createOptions() {
        def options = new JarArchiveComparatorOptions()
        options.includeSynthetic = includeSynthetic
        options.with {
            filters.getIncludes().addAll(packageIncludes.collect { new JavadocLikePackageFilter(it) })
            filters.getExcludes().addAll(packageExcludes.collect { new JavadocLikePackageFilter(it) })
            Collection<File> files = classpath ?: project.configurations.japicmp.files
            files.each {
                if (it.exists()) {
                    classPathEntries.add(it.absolutePath)
                }
            }
        }
        options
    }

    private void generateOutput(final List<JApiClass> jApiClasses) {
        // we create a dummy options because we don't want to avoid use of internal classes of JApicmp
        def options = new Options()
        options.oldArchives.add(getOldArchive())
        options.newArchives.add(getNewArchive())
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
        def generic = new GenericOutputProcessor(
                builder.classProcessors,
                builder.methodProcessors,
                builder.constructorProcessors,
                builder.beforeProcessors,
                builder.afterProcessors,
                jApiClasses)
        generic.processOutput()
    }

    public void outputProcessor(@DelegatesTo(OutputProcessorBuilder) Closure spec) {
        def copy = spec.clone()
        copy.delegate = builder
        copy()
    }
}
