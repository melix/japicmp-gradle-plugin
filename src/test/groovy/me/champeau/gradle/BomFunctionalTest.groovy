package me.champeau.gradle

import org.gradle.testkit.runner.TaskOutcome

class BomFunctionalTest extends BaseFunctionalTest {
    String testProject = 'bom-project'

    def "skip if module has not any artifact"() {
        when:
        def result = run 'japicmp'

        then:
        result.task(":japicmp").outcome == TaskOutcome.SUCCESS
        hasTextReport('UNCHANGED CLASS: PUBLIC org.apache.commons.lang3.AnnotationUtils')
        noHtmlReport()
        noRichReport()

        when:
        result = run 'japicmp'

        then:
        result.task(":japicmp").outcome == TaskOutcome.UP_TO_DATE
    }
}
