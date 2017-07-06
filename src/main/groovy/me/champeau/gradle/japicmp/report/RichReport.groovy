package me.champeau.gradle.japicmp.report

import groovy.transform.CompileStatic
import japicmp.model.JApiChangeStatus
import japicmp.model.JApiCompatibilityChange
import me.champeau.gradle.japicmp.JapicmpTask
import org.gradle.api.Action
import org.gradle.api.Task
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional

@CompileStatic
class RichReport implements Serializable {
    private final transient JapicmpTask owner;

    @Internal
    transient final ViolationsGenerator violationsGenerator = new ViolationsGenerator()

    @Internal
    transient RichReportRenderer renderer = new GroovyReportRenderer()

    RichReport(JapicmpTask owner) {
        this.owner = owner
    }

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

    void addRule(ViolationRule rule) {
        owner.doFirst(new Action<Task>() {
            @Override
            void execute(final Task task) {
                violationsGenerator.addRule(rule)
            }
        })
    }

    void addRule(JApiCompatibilityChange change, ViolationRule rule) {
        owner.doFirst(new Action<Task>() {
            @Override
            void execute(final Task task) {
                violationsGenerator.addRule(change, rule)
            }
        })
    }

    void addRule(JApiChangeStatus status, ViolationRule rule) {
        owner.doFirst(new Action<Task>() {
            @Override
            void execute(final Task task) {
                violationsGenerator.addRule(status, rule)
            }
        })
    }

    void renderer(Class<? extends RichReportRenderer> rendererType) {
        owner.doFirst(new Action<Task>() {
            @Override
            void execute(final Task task) {
                renderer = rendererType.newInstance()
            }
        })
    }
}
