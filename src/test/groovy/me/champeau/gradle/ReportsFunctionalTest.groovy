package me.champeau.gradle

import org.gradle.testkit.runner.TaskOutcome

class ReportsFunctionalTest extends BaseFunctionalTest {
    String testProject = 'html-report'

    def "can generate an HTML report"() {
        when:
        def result = run 'japicmp'

        then:
        result.task(":japicmp").outcome == TaskOutcome.SUCCESS
        hasHtmlReport("""
\t\t\t<td>Old:</td>
\t\t\t<td>
\t\t\t\tcommons-lang3-3.5.jar
\t\t\t</td>""")
        hasHtmlReport("""
\t\t\t<td>New:</td>
\t\t\t<td>
\t\t\t\tcommons-lang3-3.6.jar
\t\t\t</td>""")
        hasHtmlReport('<a href="#org.apache.commons.lang3.event.EventListenerSupport">')
        noTxtReport()
        noMarkdownReport()
        noSemverReport()
        noXmlReport()
        noRichReport()

        when:
        result = run 'japicmp'

        then:
        result.task(":japicmp").outcome == TaskOutcome.UP_TO_DATE
    }

    def "can generate an XML report"() {
        when:
        def result = run 'japicmpXml'

        then:
        result.task(":japicmpXml").outcome == TaskOutcome.SUCCESS
        hasXmlReport('oldJar="commons-lang3-3.5.jar"')
        hasXmlReport('newJar="commons-lang3-3.6.jar"')
        hasXmlReport('oldVersion="unknown version"')
        hasXmlReport('newVersion="unknown version"')
        noTxtReport()
        noMarkdownReport()
        noSemverReport()
        noHtmlReport()
        noRichReport()

        when:
        result = run 'japicmpXml'

        then:
        result.task(":japicmpXml").outcome == TaskOutcome.UP_TO_DATE
    }

    def "can generate rich report"() {
        when:
        def result = run 'japicmpRich'

        then:
        result.task(":japicmpRich").outcome == TaskOutcome.SUCCESS
        hasRichReport('<a class=\'navbar-brand\' href=\'#\'>Binary compatibility report</a>')
        hasRichReport('A test of rich report')
        noTxtReport()
        noMarkdownReport()
        noSemverReport()
        noHtmlReport()
        noXmlReport()

        when:
        result = run 'japicmpRich'

        then:
        result.task(":japicmpRich").outcome == TaskOutcome.UP_TO_DATE
    }

    def "can generate a semantic versioning report"() {
        when:
        def result = run 'japicmpSemver'

        then:
        result.task(":japicmpSemver").outcome == TaskOutcome.SUCCESS
        hasSemverReport('0.1.0')
        noTxtReport()
        noMarkdownReport()
        noHtmlReport()
        noXmlReport()
        noRichReport()

        when:
        result = run 'japicmpSemver'

        then:
        result.task(":japicmpSemver").outcome == TaskOutcome.UP_TO_DATE
    }

    def "can generate a markdown report"() {
        when:
        def result = run 'japicmpMarkdown'

        then:
        result.task(":japicmpMarkdown").outcome == TaskOutcome.SUCCESS
        hasmarkdownReport('# Compatibility Report')
        hasmarkdownReport('- **Report only summary**: No')
        noTxtReport()
        noSemverReport()
        noHtmlReport()
        noXmlReport()
        noRichReport()

        when:
        result = run 'japicmpMarkdown'

        then:
        result.task(":japicmpMarkdown").outcome == TaskOutcome.UP_TO_DATE
    }

    def "can generate a summary-only markdown report"() {
        when:
        def result = run 'japicmpMarkdownReportOnlySummary'

        then:
        result.task(":japicmpMarkdownReportOnlySummary").outcome == TaskOutcome.SUCCESS
        hasmarkdownReport('# Compatibility Report')
        hasmarkdownReport('- **Report only summary**: Yes')
        noTxtReport()
        noSemverReport()
        noHtmlReport()
        noXmlReport()
        noRichReport()

        when:
        result = run 'japicmpMarkdownReportOnlySummary'

        then:
        result.task(":japicmpMarkdownReportOnlySummary").outcome == TaskOutcome.UP_TO_DATE
    }
}
