package me.champeau.gradle

import org.gradle.testkit.runner.TaskOutcome

class SimpleFunctionalTest extends BaseFunctionalTest {
    String testProject = 'simple'

    def "can apply plugin"() {
        when:
        def result = run 'help'

        then:
        result.task(":help").outcome == TaskOutcome.SUCCESS
    }
}
