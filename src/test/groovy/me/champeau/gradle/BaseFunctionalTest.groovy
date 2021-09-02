package me.champeau.gradle

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import java.nio.file.Files

abstract class BaseFunctionalTest extends Specification {
    @Rule final TemporaryFolder testProjectDir = new TemporaryFolder()
    private static final String TEST_PROJECT_PATH = "/src/test/test-projects/"

    abstract String getTestProject()

    def setup() {
        def sourceProject = new File(".", (TEST_PROJECT_PATH+testProject).replace((char)"/", File.separatorChar)).canonicalFile
        def sourcePath = sourceProject.toPath()
        def destinationPath = testProjectDir.root.toPath()

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
        new File(testProjectDir.root, "build")
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

    void hasHtmlReport(String lookup = '') {
        hasReport('japi', 'html', lookup)
    }

    void hasRichReport(String lookup = '') {
        hasReport('rich', 'html', lookup)
    }

    void noTxtReport() {
        assert !getReport('japi', 'txt').exists()
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
                .withProjectDir(testProjectDir.root)
                .withArguments('-s', *(tasks as List))
                .withPluginClasspath()
                .build()
    }

    BuildResult fails(String... tasks) {
        GradleRunner.create()
                .withGradleVersion(gradleVersion)
                .withProjectDir(testProjectDir.root)
                .withArguments('-s', *(tasks as List))
                .withPluginClasspath()
                .buildAndFail()
    }

    String getGradleVersion() {
        System.getProperty("gradleVersion")
    }
}
