package me.champeau.gradle

import org.gradle.testkit.runner.TaskOutcome

class SourceIncompatibleChangeTest extends BaseFunctionalTest {
    String testProject = 'source-incompatible-change'

    def "source incompatibility does not break build by default"() {
        when:
        def result = run "japicmpIgnoresSourceCompatabilityByDefault"

        then:
        result.task(":japicmpIgnoresSourceCompatabilityByDefault").outcome == TaskOutcome.SUCCESS
    }

    def "source incompatibility breaks build"() {
        when:
        def result = fails "japicmpFailsOnSourceIncompatability"

        then:
        result.task(":japicmpFailsOnSourceIncompatability").outcome == TaskOutcome.FAILED
    }

    def "source incompatibility breaks build (non-public access)"() {
        when:
        def result = fails "japicmpFailsOnSourceIncompatabilityNonPublic"

        then:
        result.task(":japicmpFailsOnSourceIncompatabilityNonPublic").outcome == TaskOutcome.FAILED
    }
}
