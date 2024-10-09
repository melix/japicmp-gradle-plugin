package me.champeau.gradle

import org.gradle.testkit.runner.TaskOutcome

class Compare2JarsFunctionalTest extends BaseFunctionalTest {
    String testProject = 'compare-2-jars'

    def "can compare 2 jars using classpath property"() {
        when:
        def result = run 'japicmp'

        then:
        result.task(":japicmp").outcome == TaskOutcome.SUCCESS
        hasTextReport('Comparing source compatibility of commons-lang3-3.6.jar against commons-lang3-3.5.jar')
        hasTextReport('UNCHANGED CLASS: PUBLIC org.apache.commons.lang3.AnnotationUtils')
        noSemverReport()
        noMarkdownReport()
        noHtmlReport()
        noXmlReport()
        noRichReport()

        when:
        result = run 'japicmp'

        then:
        result.task(":japicmp").outcome == TaskOutcome.UP_TO_DATE
    }

    def "can compare 2 jars using explicit archives property"() {
        when:
        def result = run 'japicmpWithExplicitClasspath'

        then:
        result.task(":japicmpWithExplicitClasspath").outcome == TaskOutcome.SUCCESS
        hasTextReport('Comparing source compatibility of commons-lang3-3.6.jar against commons-lang3-3.5.jar')
        hasTextReport('UNCHANGED CLASS: PUBLIC org.apache.commons.lang3.AnnotationUtils')
        noSemverReport()
        noMarkdownReport()
        noHtmlReport()
        noXmlReport()
        noRichReport()

        when:
        result = run 'japicmpWithExplicitClasspath'

        then:
        result.task(":japicmpWithExplicitClasspath").outcome == TaskOutcome.UP_TO_DATE
    }
}
