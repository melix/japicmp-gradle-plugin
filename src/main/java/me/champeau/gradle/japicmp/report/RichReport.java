package me.champeau.gradle.japicmp.report;

import japicmp.model.JApiChangeStatus;
import japicmp.model.JApiCompatibilityChange;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;

import java.io.File;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public abstract class RichReport {

    public RichReport() {
        getAddDefaultRules().convention(false);
        getRenderer().convention(GroovyReportRenderer.class);
        getReportName().convention("rich-report.html");
    }

    public void addRule(Class<? extends ViolationRule> rule, Map<String, String> params) {
        getRules().add(new ViolationRuleConfiguration(rule, params));
    }

    public void addRule(Class<? extends ViolationRule> rule) {
        addRule(rule, null);
    }

    public void addSetupRule(Class<? extends SetupRule> rule, Map<String, String> params) {
        getRules().add(new SetupRuleConfiguration(rule, params));
    }

    public void addSetupRule(Class<? extends SetupRule> rule) {
        addSetupRule(rule, null);
    }

    public void addPostProcessRule(Class<? extends PostProcessViolationsRule> rule, Map<String, String> params) {
        getRules().add(new PostProcessRuleConfiguration(rule, params));
    }

    public void addPostProcessRule(Class<? extends PostProcessViolationsRule> rule) {
        addPostProcessRule(rule, null);
    }

    public void addRule(JApiCompatibilityChange change, Class<? extends ViolationRule> rule, Map<String, String> params) {
        getRules().add(new CompatibilityChangeViolationRuleConfiguration(rule, params, change));
    }

    public void addRule(JApiCompatibilityChange change, Class<? extends ViolationRule> rule) {
        addRule(change, rule, null);
    }

    public void addRule(JApiChangeStatus status, Class<? extends ViolationRule> rule, Map<String, String> params) {
        getRules().add(new StatusChangeViolationRuleConfiguration(rule, params, status));
    }

    public void addRule(JApiChangeStatus status, Class<? extends ViolationRule> rule) {
        addRule(status, rule, null);
    }

    public void renderer(Class<? extends RichReportRenderer> rendererType) {
        this.getRenderer().set(rendererType);
    }

    @Input
    public abstract Property<Class<? extends RichReportRenderer>> getRenderer();

    @Optional
    @Input
    public abstract ListProperty<String> getIncludedClasses();

    @Optional
    @Input
    public abstract ListProperty<String> getExcludedClasses();

    @Internal
    public abstract DirectoryProperty getDestinationDir();

    @Input
    public abstract Property<String> getReportName();

    @Optional
    @Input
    public abstract Property<String> getTitle();

    @Optional
    @Input
    public abstract Property<String> getDescription();

    @Input
    public abstract ListProperty<RuleConfiguration> getRules();

    @Input
    public abstract Property<Boolean> getAddDefaultRules();

    @OutputFile
    public Provider<RegularFile> getOutputFile() {
        return getDestinationDir().zip(getReportName(), Directory::file);
    }

    public Configuration toConfiguration() {
        return new Configuration(
                getReportName().get(),
                getRenderer().get(),
                getIncludedClasses().getOrElse(Collections.emptyList()),
                getExcludedClasses().getOrElse(Collections.emptyList()),
                getTitle().getOrNull(),
                getDescription().getOrNull(),
                getRules().getOrElse(Collections.emptyList()),
                getAddDefaultRules().get(),
                getOutputFile().get().getAsFile()
        );
    }

    public static final class Configuration implements Serializable {
        private final String reportName;
        private final Class<? extends RichReportRenderer> renderer;
        private final List<String> includedClasses;
        private final List<String> excludedClasses;
        private final String title;
        private final String description;
        private final List<RuleConfiguration> rules;
        private final boolean addDefaultRules;
        private final File outputFile;

        public Configuration(String reportName,
                             Class<? extends RichReportRenderer> renderer,
                             List<String> includedClasses,
                             List<String> excludedClasses,
                             String title,
                             String description,
                             List<RuleConfiguration> rules,
                             boolean addDefaultRules,
                             File outputFile) {
            this.reportName = reportName;
            this.renderer = renderer;
            this.includedClasses = includedClasses;
            this.excludedClasses = excludedClasses;
            this.title = title;
            this.description = description;
            this.rules = rules;
            this.addDefaultRules = addDefaultRules;
            this.outputFile = outputFile;
        }

        public String getReportName() {
            return reportName;
        }

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
        }

        public List<RuleConfiguration> getRules() {
            return rules;
        }

        public boolean isAddDefaultRules() {
            return addDefaultRules;
        }

        public File getOutputFile() {
            return outputFile;
        }

        public Class<? extends RichReportRenderer> getRenderer() {
            return renderer;
        }

        public List<String> getIncludedClasses() {
            return includedClasses;
        }

        public List<String> getExcludedClasses() {
            return excludedClasses;
        }

        @Override
        public String toString() {
            return "ReportConfiguration{" +
                    "reportName='" + reportName + '\'' +
                    ", renderer=" + renderer +
                    ", includedClasses=" + includedClasses +
                    ", excludedClasses=" + excludedClasses +
                    ", title='" + title + '\'' +
                    ", description='" + description + '\'' +
                    ", rules=" + rules +
                    ", addDefaultRules=" + addDefaultRules +
                    ", outputFile=" + outputFile +
                    '}';
        }
    }

}
