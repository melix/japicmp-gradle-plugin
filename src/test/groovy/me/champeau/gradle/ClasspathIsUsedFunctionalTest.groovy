package me.champeau.gradle

import org.gradle.api.JavaVersion
import org.gradle.testkit.runner.TaskOutcome

class ClasspathIsUsedFunctionalTest extends BaseFunctionalTest {
    String testProject = 'classpath-is-used'

    def "classpath property is passed to japicmp entirely"() {
        when:
        def result = run 'japicmp'

        then:
        result.task(":japicmp").outcome == TaskOutcome.SUCCESS
        def byteCodeVersion
        switch (JavaVersion.current()) {
            case JavaVersion.VERSION_1_8:
                byteCodeVersion = '52.0'
                break
            case JavaVersion.VERSION_11:
                byteCodeVersion = '55.0'
                break
            case JavaVersion.VERSION_17:
                byteCodeVersion = '61.0'
                break
            case JavaVersion.VERSION_21:
                byteCodeVersion = '65.0'
                break
            default:
                throw new IllegalStateException("Need to update the byteCode version mapping for Java ${JavaVersion.current()}, you can ref https://javaalmanac.io/bytecode/versions")
        }
        // Superclasses can only be reported if the classpath is present
        hasTextReport("Comparing source compatibility of ${testProjectDir.fileName}-v2.jar against ${testProjectDir.fileName}.jar")
        hasTextReport("""
***! MODIFIED CLASS: PUBLIC me.champeau.gradle.japicmp.Subtype  (not serializable)
\t===  CLASS FILE FORMAT VERSION: $byteCodeVersion <- $byteCodeVersion
\t***! MODIFIED SUPERCLASS: me.champeau.gradle.japicmp.ChangedLibrarySuperclass (<- me.champeau.gradle.japicmp.LibrarySuperclass)
\t===  UNCHANGED CONSTRUCTOR: PUBLIC Subtype()
""")
        noSemverReport()
        noHtmlReport()
        noRichReport()
    }

    // This is a sanity check to ensure this test is not affected by a future change in how japicmp reports superclasses
    // Technically japicmp does not need to load the class to check basic superclass compatibility, but it currently does
    def "setting no classpath property results in a failure"() {
        when:
        def result = fails 'japicmpWithoutClasspath'

        then:
        result.task(":japicmpWithoutClasspath").outcome == TaskOutcome.FAILED
        result.output.contains("javassist.NotFoundException: me.champeau.gradle.japicmp.LibrarySuperclass")
    }
}
