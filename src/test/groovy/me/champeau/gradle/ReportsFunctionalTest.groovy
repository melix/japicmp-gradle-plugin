package me.champeau.gradle

import org.gradle.testkit.runner.TaskOutcome

class ReportsFunctionalTest extends BaseFunctionalTest {
    String testProject = 'html-report'

    def "can generate an HTML report"() {
        when:
        def result = run 'japicmp'

        then:
        result.task(":japicmp").outcome == TaskOutcome.SUCCESS
        hasHtmlReport('<td>Old:</td><td>commons-lang3-3.5.jar</td>')
        hasHtmlReport('<td>New:</td><td>commons-lang3-3.6.jar</td>')
        hasHtmlReport('<a href="#org.apache.commons.lang3.event.EventListenerSupport">')
        noTxtReport()
        noRichReport()

        when:
        result = run 'japicmp'

        then:
        result.task(":japicmp").outcome == TaskOutcome.UP_TO_DATE
    }

    def "can generate rich report"() {
        when:
        def result = run 'japicmpRich'

        then:
        result.task(":japicmpRich").outcome == TaskOutcome.SUCCESS
        hasRichReport('<a class=\'navbar-brand\' href=\'#\'>Binary compatibility report</a>')
        hasRichReport('A test of rich report')
        noTxtReport()
        noHtmlReport()

        when:
        result = run 'japicmpRich'

        then:
        result.task(":japicmpRich").outcome == TaskOutcome.UP_TO_DATE
    }
}
