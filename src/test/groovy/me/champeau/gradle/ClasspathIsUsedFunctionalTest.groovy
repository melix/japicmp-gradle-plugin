package me.champeau.gradle

import org.gradle.testkit.runner.TaskOutcome

class ClasspathIsUsedFunctionalTest extends BaseFunctionalTest {
    String testProject = 'classpath-is-used'

    def "classpath property is passed to japicmp entirely"() {
        when:
        def result = run 'japicmp'

        then:
        result.task(":japicmp").outcome == TaskOutcome.SUCCESS
        // Superclasses can only be reported if the classpath is present
        hasTextReport('''
***! MODIFIED CLASS: PUBLIC me.champeau.gradle.japicmp.Subtype  (not serializable)
\t===  CLASS FILE FORMAT VERSION: 61.0 <- 61.0
\t***! MODIFIED SUPERCLASS: me.champeau.gradle.japicmp.ChangedLibrarySuperclass (<- me.champeau.gradle.japicmp.LibrarySuperclass)
\t===  UNCHANGED CONSTRUCTOR: PUBLIC Subtype()
''')
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
