package me.champeau.gradle.japicmp.report;

import japicmp.model.JApiChangeStatus;
import japicmp.model.JApiCompatibilityChange;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RichReport implements Serializable {

    private Class<? extends RichReportRenderer> renderer = GroovyReportRenderer.class;
    private List<String> includedClasses;
    private List<String> excludedClasses;
    private File destinationDir;
    private String reportName = "rich-report.html";
    private String title;
    private String description;
    private List<RuleConfiguration> rules = new ArrayList<RuleConfiguration>();

    public void addRule(Class<? extends ViolationRule> rule, Map<String, String> params) {
        rules.add(new ViolationRuleConfiguration(rule, params));
    }

    public void addRule(Class<? extends ViolationRule> rule) {
        addRule(rule, null);
    }

    public void addSetupRule(Class<? extends SetupRule> rule, Map<String, String> params) {
        rules.add(new SetupRuleConfiguration(rule, params));
    }

    public void addSetupRule(Class<? extends SetupRule> rule) {
        addSetupRule(rule, null);
    }

    public void addPostProcessRule(Class<? extends PostProcessViolationsRule> rule, Map<String, String> params) {
        rules.add(new PostProcessRuleConfiguration(rule, params));
    }

    public void addPostProcessRule(Class<? extends PostProcessViolationsRule> rule) {
        addPostProcessRule(rule, null);
    }

    public void addRule(JApiCompatibilityChange change, Class<? extends ViolationRule> rule, Map<String, String> params) {
        rules.add(new CompatibilityChangeViolationRuleConfiguration(rule, params, change));
    }

    public void addRule(JApiCompatibilityChange change, Class<? extends ViolationRule> rule) {
        addRule(change, rule, null);
    }

    public void addRule(JApiChangeStatus status, Class<? extends ViolationRule> rule, Map<String, String> params) {
        rules.add(new StatusChangeViolationRuleConfiguration(rule, params, status));
    }

    public void addRule(JApiChangeStatus status, Class<? extends ViolationRule> rule) {
        addRule(status, rule, null);
    }

    public void renderer(Class<? extends RichReportRenderer> rendererType) {
        this.renderer = rendererType;
    }

    @Input
    public Class<? extends RichReportRenderer> getRenderer() {
        return renderer;
    }

    public void setRenderer(Class<? extends RichReportRenderer> renderer) {
        this.renderer = renderer;
    }

    @Optional
    @Input
    public List<String> getIncludedClasses() {
        return includedClasses;
    }

    public void setIncludedClasses(List<String> includedClasses) {
        this.includedClasses = includedClasses;
    }

    @Optional
    @Input
    public List<String> getExcludedClasses() {
        return excludedClasses;
    }

    public void setExcludedClasses(List<String> excludedClasses) {
        this.excludedClasses = excludedClasses;
    }

    @Optional
    @OutputDirectory
    public File getDestinationDir() {
        return destinationDir;
    }

    public void setDestinationDir(File destinationDir) {
        this.destinationDir = destinationDir;
    }

    @Optional
    @Input
    public String getReportName() {
        return reportName;
    }

    public void setReportName(String reportName) {
        this.reportName = reportName;
    }

    @Optional
    @Input
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Optional
    @Input
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Input
    public List<RuleConfiguration> getRules() {
        return rules;
    }

    public void setRules(List<RuleConfiguration> rules) {
        this.rules = rules;
    }


}
