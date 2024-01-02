package me.champeau.gradle

import org.gradle.testkit.runner.TaskOutcome

class Compare2LibrariesFunctionalTest extends BaseFunctionalTest {
    String testProject = 'compare-2-libraries'

    def "can compare 2 libraries using classpath property"() {
        when:
        def result = run 'japicmp'

        then:
        result.task(":japicmp").outcome == TaskOutcome.SUCCESS
        hasTextReport('UNCHANGED CLASS: PUBLIC org.apache.commons.lang3.AnnotationUtils')
        noSemverReport()
        noHtmlReport()
        noRichReport()

        when:
        result = run 'japicmp'

        then:
        result.task(":japicmp").outcome == TaskOutcome.UP_TO_DATE
    }
}
