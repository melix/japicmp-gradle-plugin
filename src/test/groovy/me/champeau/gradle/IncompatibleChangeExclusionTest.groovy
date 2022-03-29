package me.champeau.gradle

import org.gradle.testkit.runner.TaskOutcome

class IncompatibleChangeExclusionTest extends BaseFunctionalTest {
    String testProject = 'incompatible-change-exclusion'

    def "incompatible change detected by default"() {
        when:
        def result = fails "japicmpDetectsNewDefaultMethodByDefault"

        then:
        result.task(":japicmpDetectsNewDefaultMethodByDefault").outcome == TaskOutcome.FAILED

        when:
        result = fails 'japicmpDetectsNewDefaultMethodByDefault'

        then:
        result.task(":japicmpDetectsNewDefaultMethodByDefault").outcome == TaskOutcome.FAILED
    }

    def "incompatible change can be excluded"() {
        when:
        def result = run "configuredToIgnoreNewDefaultMethod"

        then:
        result.task(":configuredToIgnoreNewDefaultMethod").outcome == TaskOutcome.SUCCESS

        when:
        result = run 'configuredToIgnoreNewDefaultMethod'

        then:
        result.task(":configuredToIgnoreNewDefaultMethod").outcome == TaskOutcome.UP_TO_DATE
    }

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
