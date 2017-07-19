package me.champeau.gradle

import org.gradle.testkit.runner.TaskOutcome

class DefaultRulesRichReportFunctionalTest extends BaseFunctionalTest {
    String testProject = 'rich-report2'

    def "can generate rich report with default rules"() {
        when:
        def result = fails "japicmp"

        then:
        result.task(":japicmp").outcome == TaskOutcome.FAILED
        def report = getReport('rich', 'html').text
        report =~ '<a class=\'navbar-brand\' href=\'#\'>Binary compatibility report</a>'
        report =~ 'Class org.apache.commons.lang3.time.FastDateFormat: Is not binary compatible'
        report =~ 'Class org.apache.commons.lang3.CharSetUtils: has been modified in source compatible way'
    }

}
