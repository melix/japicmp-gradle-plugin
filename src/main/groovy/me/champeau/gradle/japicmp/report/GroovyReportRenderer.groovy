package me.champeau.gradle.japicmp.report

import groovy.text.markup.MarkupTemplateEngine
import groovy.text.markup.TemplateConfiguration
import groovy.transform.CompileStatic

@CompileStatic
class GroovyReportRenderer implements RichReportRenderer {
    void render(File htmlReportFile, RichReportData data) {
        def templateConfiguration = new TemplateConfiguration()
        templateConfiguration.with {
            autoIndent = true
            autoNewLine = true
        }
        def engine = new MarkupTemplateEngine(this.class.classLoader, templateConfiguration)
        def template = engine.createTemplate(this.class.getResource('/templates/default.groovy'))
        def model = [title   : data.reportTitle?:'Binary compatibility report',
                     description: data.description?:'',
                     violations: data.violations
        ]

        htmlReportFile.withWriter('utf-8') { wrt ->
            template.make(model).writeTo(wrt)
        }
    }

}
