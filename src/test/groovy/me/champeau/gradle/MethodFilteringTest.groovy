package me.champeau.gradle

import org.gradle.testkit.runner.TaskOutcome

class MethodFilteringTest extends BaseFunctionalTest {
    String testProject = 'filtering/method'

    def "including only bad method"() {
        when:
        def result = run 'japicmpMethodIncludeOnlyBad'

        then:
        result.task(":japicmpMethodIncludeOnlyBad").outcome == TaskOutcome.SUCCESS
        hasTextReport('MODIFIED METHOD: PUBLIC void (<-int) getInteger()')
        def report = getReport('japi', 'txt').text
        !report.contains('UNCHANGED METHOD: PUBLIC void unchanged()')

        when:
        result = run 'japicmpMethodIncludeOnlyBad'

        then:
        result.task(":japicmpMethodIncludeOnlyBad").outcome == TaskOutcome.UP_TO_DATE
    }

    def "including only good method"() {
        when:
        def result = run 'japicmpMethodIncludeOnlyGood'

        then:
        result.task(":japicmpMethodIncludeOnlyGood").outcome == TaskOutcome.SUCCESS
        hasTextReport('UNCHANGED METHOD: PUBLIC void unchanged()')
        def report = getReport('japi', 'txt').text
        !report.contains('MODIFIED METHOD: PUBLIC void (<-int) getInteger()')

        when:
        result = run 'japicmpMethodIncludeOnlyGood'

        then:
        result.task(":japicmpMethodIncludeOnlyGood").outcome == TaskOutcome.UP_TO_DATE
    }

    def "excluding good method"() {
        when:
        def result = run 'japiCmpMethodExcludeKeepBad'

        then:
        result.task(":japiCmpMethodExcludeKeepBad").outcome == TaskOutcome.SUCCESS
        hasTextReport('MODIFIED METHOD: PUBLIC void (<-int) getInteger()')
        def report = getReport('japi', 'txt').text
        !report.contains('UNCHANGED METHOD: PUBLIC void unchanged()')

        when:
        result = run 'japiCmpMethodExcludeKeepBad'

        then:
        result.task(":japiCmpMethodExcludeKeepBad").outcome == TaskOutcome.UP_TO_DATE
    }

    def "excluding bad method"() {
        when:
        def result = run 'japicmpMethodExcludeKeepGood'

        then:
        result.task(":japicmpMethodExcludeKeepGood").outcome == TaskOutcome.SUCCESS
        hasTextReport('UNCHANGED METHOD: PUBLIC void unchanged()')
        def report = getReport('japi', 'txt').text
        !report.contains('MODIFIED METHOD: PUBLIC void (<-int) getInteger()')

        when:
        result = run 'japicmpMethodExcludeKeepGood'

        then:
        result.task(":japicmpMethodExcludeKeepGood").outcome == TaskOutcome.UP_TO_DATE
    }
}
