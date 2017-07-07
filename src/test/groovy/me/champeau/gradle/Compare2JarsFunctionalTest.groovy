package me.champeau.gradle

import org.gradle.testkit.runner.TaskOutcome

class Compare2JarsFunctionalTest extends BaseFunctionalTest {
    String testProject = 'compare-2-jars'

    def "can compare 2 jars using classpath property"() {
        when:
        def result = run 'japicmp'

        then:
        result.task(":japicmp").outcome == TaskOutcome.SUCCESS
        hasTextReport('UNCHANGED CLASS: PUBLIC org.apache.commons.lang3.AnnotationUtils')
        noHtmlReport()
        noRichReport()
    }

    def "can compare 2 jars using explicit archives property"() {
        when:
        def result = run 'japicmpWithExplicitClasspath'

        then:
        result.task(":japicmpWithExplicitClasspath").outcome == TaskOutcome.SUCCESS
        hasTextReport('UNCHANGED CLASS: PUBLIC org.apache.commons.lang3.AnnotationUtils')
        noHtmlReport()
        noRichReport()
    }
}
