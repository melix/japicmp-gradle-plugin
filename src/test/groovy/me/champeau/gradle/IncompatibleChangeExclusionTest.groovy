package me.champeau.gradle

import org.gradle.testkit.runner.TaskOutcome

class IncompatibleChangeExclusionTest extends BaseFunctionalTest {
    String testProject = 'incompatible-change-exclusion'

    def "bad incompatible change name is reported"() {
        when:
        def result = fails "configuredWithBadCompatibilityChange"

        then:
        result.task(":configuredWithBadCompatibilityChange").outcome == TaskOutcome.FAILED

        when:
        result = fails 'configuredWithBadCompatibilityChange'

        then:
        result.task(":configuredWithBadCompatibilityChange").outcome == TaskOutcome.FAILED
    }
}
