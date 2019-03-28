package me.champeau.gradle

import org.gradle.testkit.runner.TaskOutcome


class CustomFilteringTest extends BaseFunctionalTest {

    @Override
    String getTestProject() {
        return "filtering/custom"
    }

    def "can use custom japicmp filters"() {
        when:
        def result = run 'japicmpAddCustomMatchTriggers'

        then:
        println(result.output)
        result.task(":japicmpAddCustomMatchTriggers").outcome == TaskOutcome.SUCCESS
        hasTextReport('REMOVED FIELD: PUBLIC(-) java.lang.String someField')
        hasTextReport('REMOVED METHOD: PUBLIC(-) void someMethod()')

        when:
        result = run 'japicmpRemoveCustomMatchTriggers'

        then:
        println(result.output)
        result.task(":japicmpRemoveCustomMatchTriggers").outcome == TaskOutcome.SUCCESS
        hasTextReport('NEW FIELD: PUBLIC(+) java.lang.String someField')
        hasTextReport('NEW METHOD: PUBLIC(+) void someMethod()')
    }
}
