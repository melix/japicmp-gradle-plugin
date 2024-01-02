package me.champeau.gradle

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Files
import java.nio.file.Path

abstract class BaseFunctionalTest extends Specification {
    @TempDir Path testProjectDir

    private static final String TEST_PROJECT_PATH = "/src/test/test-projects/"

    abstract String getTestProject()

    def setup() {
        def sourceProject = new File(".", (TEST_PROJECT_PATH+testProject).replace((char)"/", File.separatorChar)).canonicalFile
        def sourcePath = sourceProject.toPath()
        def destinationPath = testProjectDir

        sourceProject.eachFileRecurse { f ->
            def path = f.toPath()
            def relativePath = sourcePath.relativize(path)
            def targetPath = destinationPath.resolve(relativePath)
            if (f.directory) {
                targetPath.toFile().mkdirs()
            } else {
                Files.copy(path, targetPath)
            }
        }
    }

    File getBuildDir() {
        testProjectDir.resolve("build").toFile()
    }

    File getReportsDir(String reportsDirName = 'reports') {
        new File(buildDir, reportsDirName)
    }

    File getReport(String basename, String ext='txt') {
        new File(reportsDir, "${basename}.${ext}")
    }

    void hasReport(String report='japi', String ext='', String lookup = '') {
        if (lookup) {
            assert getReport(report, ext)?.text?.contains(lookup)
        } else {
            assert getReport(report, ext)?.text
        }
    }

    void hasTextReport(String lookup = '') {
        hasReport('japi', 'txt', lookup)
    }

    void hasSemverReport(String lookup = '') {
        hasReport('japi', 'semver', lookup)
    }

    void hasHtmlReport(String lookup = '') {
        hasReport('japi', 'html', lookup)
    }

    void hasRichReport(String lookup = '') {
        hasReport('rich', 'html', lookup)
    }

    void noTxtReport() {
        assert !getReport('japi', 'txt').exists()
    }

    void noSemverReport() {
        assert !getReport('japi', 'semver').exists()
    }

    void noHtmlReport() {
        assert !getReport('japi', 'html').exists()
    }

    void noRichReport() {
        assert !getReport('rich', 'html').exists()
    }

    BuildResult run(String... tasks) {
        GradleRunner.create()
                .withGradleVersion(gradleVersion)
                .withProjectDir(testProjectDir.toFile())
                .withArguments(*(extraArguments + (tasks as List)))
                .forwardOutput()
                .withPluginClasspath()
                .build()
    }

    BuildResult fails(String... tasks) {
        GradleRunner.create()
                .withGradleVersion(gradleVersion)
                .withProjectDir(testProjectDir.toFile())
                .withArguments(*(extraArguments + (tasks as List)))
                .withPluginClasspath()
                .forwardOutput()
                .buildAndFail()
    }

    private String getGradleVersion() {
        System.getProperty("gradleVersion")
    }

    protected boolean supportsConfigurationCache = true

    private List<String> getExtraArguments() {
        def extraArgs = ['--stacktrace', '--warning-mode=fail']
        // TODO: https://github.com/melix/japicmp-gradle-plugin/commit/c398274f8e0a3c3daad7475c420806cebbef190e
        if (supportsConfigurationCache) {
            extraArgs << '--configuration-cache'
        }
        return extraArgs
    }
}
