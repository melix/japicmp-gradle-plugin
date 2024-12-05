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

    def "can generate an XML report without versions"() {
        when:
        def result = run 'japicmpXmlWithoutVersions'

        then:
        result.task(":japicmpXmlWithoutVersions").outcome == TaskOutcome.SUCCESS
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
        result = run 'japicmpXmlWithoutVersions'

        then:
        result.task(":japicmpXmlWithoutVersions").outcome == TaskOutcome.UP_TO_DATE
    }

    def "can generate an XML report with versions"() {
        when:
        def result = run 'japicmpXmlWithVersions'

        then:
        result.task(":japicmpXmlWithVersions").outcome == TaskOutcome.SUCCESS
        hasXmlReport('oldJar="commons-lang3-3.5.jar"')
        hasXmlReport('newJar="commons-lang3-3.6.jar"')
        hasXmlReport('oldVersion="3.5"')
        hasXmlReport('newVersion="3.6"')
        noTxtReport()
        noMarkdownReport()
        noSemverReport()
        noHtmlReport()
        noRichReport()

        when:
        result = run 'japicmpXmlWithVersions'

        then:
        result.task(":japicmpXmlWithVersions").outcome == TaskOutcome.UP_TO_DATE
    }

    def "can generate an XML report with no duplicate transitive versions"() {
        when:
        def result = run 'japicmpXmlWithTransitiveVersions'

        then:
        result.task(":japicmpXmlWithTransitiveVersions").outcome == TaskOutcome.SUCCESS
        hasXmlReport('oldJar="assertj-guava-3.25.3.jar;assertj-core-3.25.3.jar;byte-buddy-1.14.11.jar;assertj-joda-time-2.2.0.jar"')
        hasXmlReport('newJar="assertj-guava-3.26.3.jar;assertj-core-3.26.3.jar;byte-buddy-1.14.18.jar;assertj-joda-time-2.1.0.jar"')
        hasXmlReport('oldVersion="3.25.3;3.25.3;1.14.11;2.2.0"')
        hasXmlReport('newVersion="3.26.3;3.26.3;1.14.18;2.1.0"')

        noTxtReport()
        noMarkdownReport()
        noSemverReport()
        noHtmlReport()
        noRichReport()

        when:
        result = run 'japicmpXmlWithTransitiveVersions'

        then:
        result.task(":japicmpXmlWithTransitiveVersions").outcome == TaskOutcome.UP_TO_DATE
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
