package me.champeau.gradle.japicmp.report;

import groovy.text.Template;
import groovy.text.markup.MarkupTemplateEngine;
import groovy.text.markup.TemplateConfiguration;
import org.gradle.api.GradleException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.LinkedHashMap;

public class GroovyReportRenderer implements RichReportRenderer {
    public void render(File htmlReportFile, RichReportData data) {
        TemplateConfiguration templateConfiguration = new TemplateConfiguration();
        templateConfiguration.setAutoNewLine(true);
        templateConfiguration.setAutoIndent(true);
        MarkupTemplateEngine engine = new MarkupTemplateEngine(this.getClass().getClassLoader(), templateConfiguration);
        Template template = null;
        try {
            template = engine.createTemplate(this.getClass().getResource("/templates/default.groovy"));
        } catch (IOException | ClassNotFoundException e) {
            throw new GradleException("Unable to load report template", e);
        }
        LinkedHashMap<String, Object> model = new LinkedHashMap<String, Object>(3);
        String title = data.getReportTitle();
        model.put("title", title != null ? title : "Binary compatibility report");
        String description = data.getDescription();
        model.put("description", description != null ? description : "");
        model.put("violations", data.getViolations());
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(htmlReportFile), "utf-8"
        ))) {
            template.make(model).writeTo(writer);
        } catch (IOException e) {
            throw new GradleException("Unable to write report", e);
        }
    }
}
