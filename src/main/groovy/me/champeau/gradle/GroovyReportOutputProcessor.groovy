package me.champeau.gradle

import groovy.text.markup.MarkupTemplateEngine
import groovy.text.markup.TemplateConfiguration
import groovy.transform.CompileStatic

@CompileStatic
class GroovyReportOutputProcessor {
    public void render(File htmlReportFile) {
        def templateConfiguration = new TemplateConfiguration()
        templateConfiguration.with {
            autoIndent = true
            autoNewLine = true
        }
        def engine = new MarkupTemplateEngine(this.class.classLoader, templateConfiguration)
        def template = engine.createTemplate(this.class.getResource('/templates/default.groovy'))
        def model = [title   : "Binary compatibility report for XXX",
                     project : 'XXX',
                     baseline: 'XXX',
                     archive : 'XXX',
                     violations: [:]
        ]

        htmlReportFile.withWriter('utf-8') { wrt ->
            template.make(model).writeTo(wrt)
        }
    }

}
