package me.champeau.gradle

import org.gradle.testkit.runner.TaskOutcome

class BomFunctionalTest extends BaseFunctionalTest {
    String testProject = 'bom-project'

    def "skip if module has not any artifact"() {
        when:
        def result = run 'japicmp'

        then:
        result.task(":japicmp").outcome == TaskOutcome.SUCCESS
        hasTextReport('CLASS FILE FORMAT VERSION: 50.0 <- 50.0')
        noSemverReport()
        noHtmlReport()
        noRichReport()

        when:
        result = run 'japicmp'

        then:
        result.task(":japicmp").outcome == TaskOutcome.UP_TO_DATE
    }
}
