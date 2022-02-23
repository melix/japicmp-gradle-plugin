package me.champeau.gradle

import org.gradle.testkit.runner.TaskOutcome
import spock.lang.Requires

@Requires({
    def major = System.getProperty("gradleVersion").split("\\.").first() as int
    major >= 6
})
class BomFunctionalTest extends BaseFunctionalTest {
    String testProject = 'bom-project'

    def "skip if module has not any artifact"() {
        when:
        def result = run 'japicmp'

        then:
        result.task(":japicmp").outcome == TaskOutcome.SUCCESS
        hasTextReport('CLASS FILE FORMAT VERSION: 50.0 <- 50.0')
        noHtmlReport()
        noRichReport()

        when:
        result = run 'japicmp'

        then:
        result.task(":japicmp").outcome == TaskOutcome.UP_TO_DATE
    }
}
