package me.champeau.gradle

import org.gradle.testkit.runner.TaskOutcome

class FieldFilteringTest extends BaseFunctionalTest {
    String testProject = 'filtering/field'

    def "including only bad field"() {
        when:
        def result = run 'japicmpFieldIncludeOnlyBad'

        then:
        result.task(":japicmpFieldIncludeOnlyBad").outcome == TaskOutcome.SUCCESS
        hasTextReport('MODIFIED FIELD: PUBLIC java.lang.String (<- int) bad')
        def report = getReport('japi', 'txt').text
        !report.contains('UNCHANGED FIELD: PUBLIC java.lang.String unchanged')

        when:
        result = run 'japicmpFieldIncludeOnlyBad'

        then:
        result.task(":japicmpFieldIncludeOnlyBad").outcome == TaskOutcome.UP_TO_DATE
    }

    def "including only good field"() {
        when:
        def result = run 'japicmpFieldIncludeOnlyGood'

        then:
        result.task(":japicmpFieldIncludeOnlyGood").outcome == TaskOutcome.SUCCESS
        hasTextReport('UNCHANGED FIELD: PUBLIC java.lang.String unchanged')
        def report = getReport('japi', 'txt').text
        !report.contains('MODIFIED FIELD: PUBLIC java.lang.String (<- int) bad')

        when:
        result = run 'japicmpFieldIncludeOnlyGood'

        then:
        result.task(":japicmpFieldIncludeOnlyGood").outcome == TaskOutcome.UP_TO_DATE
    }

    def "excluding good field"() {
        when:
        def result = run 'japiCmpFieldExcludeKeepBad'

        then:
        result.task(":japiCmpFieldExcludeKeepBad").outcome == TaskOutcome.SUCCESS
        hasTextReport('MODIFIED FIELD: PUBLIC java.lang.String (<- int) bad')
        def report = getReport('japi', 'txt').text
        !report.contains('UNCHANGED FIELD: PUBLIC java.lang.String unchanged')

        when:
        result = run 'japiCmpFieldExcludeKeepBad'

        then:
        result.task(":japiCmpFieldExcludeKeepBad").outcome == TaskOutcome.UP_TO_DATE
    }

    def "excluding bad field"() {
        when:
        def result = run 'japicmpFieldExcludeKeepGood'

        then:
        result.task(":japicmpFieldExcludeKeepGood").outcome == TaskOutcome.SUCCESS
        hasTextReport('UNCHANGED FIELD: PUBLIC java.lang.String unchanged')
        def report = getReport('japi', 'txt').text
        !report.contains('MODIFIED FIELD: PUBLIC java.lang.String (<- int) bad')

        when:
        result = run 'japicmpFieldExcludeKeepGood'

        then:
        result.task(":japicmpFieldExcludeKeepGood").outcome == TaskOutcome.UP_TO_DATE
    }
}
