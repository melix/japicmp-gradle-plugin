package me.champeau.gradle

import org.gradle.testkit.runner.TaskOutcome

class AnnotationFilteringTest extends BaseFunctionalTest {
    String testProject = 'filtering/annotation'

    def "checking only @StableApi ignores other methods"() {
        when:
        def result = run 'japicmpOnlyCheckStableApi'

        then:
        result.task(":japicmpOnlyCheckStableApi").outcome == TaskOutcome.SUCCESS
        hasTextReport('MODIFIED METHOD: PUBLIC void (<-int) stableMethod()')
        def report = getReport('japi', 'txt').text
        !report.contains('MODIFIED METHOD: PUBLIC void (<-int) betaMethod()')
    }

    def "excluding @BetaApi hides beta methods"() {
        when:
        def result = run 'japicmpExcludeBetaApi'

        then:
        result.task(":japicmpExcludeBetaApi").outcome == TaskOutcome.SUCCESS
        hasTextReport('MODIFIED METHOD: PUBLIC void (<-int) stableMethod()')
        def report = getReport('japi', 'txt').text
        !report.contains('MODIFIED METHOD: PUBLIC void (<-int) betaMethod()')
    }
}
