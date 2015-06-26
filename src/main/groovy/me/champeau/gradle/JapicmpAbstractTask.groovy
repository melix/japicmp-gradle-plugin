package me.champeau.gradle

import japicmp.cmp.JarArchiveComparator
import japicmp.cmp.JarArchiveComparatorOptions
import japicmp.config.Options
import japicmp.filter.PackageFilter
import japicmp.model.AccessModifier
import japicmp.model.JApiChangeStatus
import japicmp.model.JApiClass
import japicmp.output.stdout.StdoutOutputGenerator
import japicmp.output.xml.XmlOutputGenerator
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

    @OutputFile
    @Optional
    File xmlOutputFile

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
            filters.includes.addAll(packageIncludes.collect { new PackageFilter(it) })
            filters.excludes.addAll(packageExcludes.collect { new PackageFilter(it) })
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
        options.outputOnlyModifications = onlyModified
        options.includeSynthetic = includeSynthetic
        options.setAccessModifier(AccessModifier.valueOf(accessModifier.toUpperCase()))
        if (xmlOutputFile) {
            def xmlOutputGenerator = new XmlOutputGenerator()
            xmlOutputGenerator.generate(getOldArchive(), getNewArchive(), jApiClasses, options)
        }
        if (txtOutputFile) {
            StdoutOutputGenerator stdoutOutputGenerator = new StdoutOutputGenerator(options, jApiClasses, getOldArchive(), getNewArchive())
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
