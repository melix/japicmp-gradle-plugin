package me.champeau.gradle

import org.gradle.testkit.runner.TaskOutcome

class PackageAndClassFilteringTest extends BaseFunctionalTest {
    String testProject = 'filtering/packageAndClass'

    def "including only bad package"() {
        when:
        def result = run 'japicmpPackageIncludeOnlyBad'

        then:
        result.task(":japicmpPackageIncludeOnlyBad").outcome == TaskOutcome.SUCCESS
        hasTextReport('MODIFIED CLASS: PUBLIC me.champeau.gradle.japicmp.bad.Bad')
        def report = getReport('japi', 'txt').text
        !report.contains('me.champeau.gradle.japicmp.good.Good')
    }

    def "including only good package"() {
        when:
        def result = run 'japicmpPackageIncludeOnlyGood'

        then:
        result.task(":japicmpPackageIncludeOnlyGood").outcome == TaskOutcome.SUCCESS
        hasTextReport('UNCHANGED CLASS: PUBLIC me.champeau.gradle.japicmp.good.Good')
        def report = getReport('japi', 'txt').text
        !report.contains('me.champeau.gradle.japicmp.bad.Bad')
    }

    def "excluding good package"() {
        when:
        def result = run 'japiCmpPackageExcludeKeepBad'

        then:
        result.task(":japiCmpPackageExcludeKeepBad").outcome == TaskOutcome.SUCCESS
        hasTextReport('MODIFIED CLASS: PUBLIC me.champeau.gradle.japicmp.bad.Bad')
        def report = getReport('japi', 'txt').text
        !report.contains('me.champeau.gradle.japicmp.good.Good')
    }

    def "excluding bad package"() {
        when:
        def result = run 'japicmpPackageExcludeKeepGood'

        then:
        result.task(":japicmpPackageExcludeKeepGood").outcome == TaskOutcome.SUCCESS
        hasTextReport('UNCHANGED CLASS: PUBLIC me.champeau.gradle.japicmp.good.Good')
        def report = getReport('japi', 'txt').text
        !report.contains('me.champeau.gradle.japicmp.bad.Bad')
    }

    def "including only bad class"() {
        when:
        def result = run 'japicmpClassIncludeOnlyBad'

        then:
        result.task(":japicmpClassIncludeOnlyBad").outcome == TaskOutcome.SUCCESS
        hasTextReport('MODIFIED CLASS: PUBLIC me.champeau.gradle.japicmp.bad.Bad')
        def report = getReport('japi', 'txt').text
        !report.contains('me.champeau.gradle.japicmp.good.Good')
    }

    def "including only good class"() {
        when:
        def result = run 'japicmpClassIncludeOnlyGood'

        then:
        result.task(":japicmpClassIncludeOnlyGood").outcome == TaskOutcome.SUCCESS
        hasTextReport('UNCHANGED CLASS: PUBLIC me.champeau.gradle.japicmp.good.Good')
        def report = getReport('japi', 'txt').text
        !report.contains('me.champeau.gradle.japicmp.bad.Bad')
    }

    def "excluding good class"() {
        when:
        def result = run 'japiCmpClassExcludeKeepBad'

        then:
        result.task(":japiCmpClassExcludeKeepBad").outcome == TaskOutcome.SUCCESS
        hasTextReport('MODIFIED CLASS: PUBLIC me.champeau.gradle.japicmp.bad.Bad')
        def report = getReport('japi', 'txt').text
        !report.contains('me.champeau.gradle.japicmp.good.Good')
    }

    def "excluding bad class"() {
        when:
        def result = run 'japicmpClassExcludeKeepGood'

        then:
        result.task(":japicmpClassExcludeKeepGood").outcome == TaskOutcome.SUCCESS
        hasTextReport('UNCHANGED CLASS: PUBLIC me.champeau.gradle.japicmp.good.Good')
        def report = getReport('japi', 'txt').text
        !report.contains('me.champeau.gradle.japicmp.bad.Bad')
    }
}
