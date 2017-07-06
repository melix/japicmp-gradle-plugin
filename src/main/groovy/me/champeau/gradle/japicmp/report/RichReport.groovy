package me.champeau.gradle.japicmp.report

import groovy.transform.CompileStatic
import japicmp.model.JApiChangeStatus
import japicmp.model.JApiCompatibilityChange
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

@CompileStatic
class RichReport implements Serializable {
    @Input
    Class<? extends RichReportRenderer> renderer = GroovyReportRenderer

    @Optional
    @Input
    List<String> includedClasses

    @Optional
    @Input
    List<String> excludedClasses

    @Optional
    @Input
    File destinationDir

    @Optional
    @Input
    String reportName = 'rich-report.html'

    @Optional
    @Input
    String title

    @Optional
    @Input
    String description

    @Input
    List<ViolationRuleConfiguration> rules = []

    void addRule(Class<? extends ViolationRule> rule, Map<String, String> params = null) {
        rules.add(new ViolationRuleConfiguration(rule, params))
    }

    void addRule(JApiCompatibilityChange change, Class<? extends ViolationRule> rule, Map<String, String> params = null) {
        rules.add(new CompatibilityChangeViolationRuleConfiguration(rule, params, change))
    }

    void addRule(JApiChangeStatus status, Class<? extends ViolationRule> rule, Map<String, String> params = null) {
        rules.add(new StatusChangeViolationRuleConfiguration(rule, params, status));
    }

    void renderer(Class<? extends RichReportRenderer> rendererType) {
        this.renderer = rendererType
    }
}
