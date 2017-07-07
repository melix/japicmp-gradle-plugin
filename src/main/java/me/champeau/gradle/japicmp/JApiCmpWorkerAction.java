/*
 * Copyright 2003-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.champeau.gradle.japicmp;

import japicmp.cmp.JApiCmpArchive;
import japicmp.cmp.JarArchiveComparator;
import japicmp.cmp.JarArchiveComparatorOptions;
import japicmp.config.Options;
import japicmp.filter.JavadocLikePackageFilter;
import japicmp.model.AccessModifier;
import japicmp.model.JApiClass;
import japicmp.output.stdout.StdoutOutputGenerator;
import japicmp.output.xml.XmlOutput;
import japicmp.output.xml.XmlOutputGenerator;
import japicmp.output.xml.XmlOutputGeneratorOptions;
import me.champeau.gradle.japicmp.report.CompatibilityChangeViolationRuleConfiguration;
import me.champeau.gradle.japicmp.report.RichReportData;
import me.champeau.gradle.japicmp.report.Severity;
import me.champeau.gradle.japicmp.report.StatusChangeViolationRuleConfiguration;
import me.champeau.gradle.japicmp.report.Violation;
import me.champeau.gradle.japicmp.report.ViolationRule;
import me.champeau.gradle.japicmp.report.ViolationRuleConfiguration;
import me.champeau.gradle.japicmp.report.ViolationsGenerator;
import org.gradle.api.GradleException;

import javax.inject.Inject;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class JApiCmpWorkerAction extends JapiCmpWorkerConfiguration implements Runnable {

    @Inject
    public JApiCmpWorkerAction(final JapiCmpWorkerConfiguration configuration) {
        super(configuration.includeSynthetic,
                configuration.ignoreMissingClasses,
                configuration.packageIncludes,
                configuration.packageExcludes,
                configuration.oldClasspath,
                configuration.newClasspath,
                configuration.oldArchives,
                configuration.newArchives,
                configuration.onlyModified,
                configuration.onlyBinaryIncompatibleModified,
                configuration.accessModifier,
                configuration.xmlOutputFile,
                configuration.htmlOutputFile,
                configuration.txtOutputFile,
                configuration.failOnModification,
                configuration.buildDir,
                configuration.richReport);
    }

    private JarArchiveComparatorOptions createOptions() {
        JarArchiveComparatorOptions options = new JarArchiveComparatorOptions();
        options.setClassPathMode(JarArchiveComparatorOptions.ClassPathMode.TWO_SEPARATE_CLASSPATHS);
        options.setIncludeSynthetic(includeSynthetic);
        options.getIgnoreMissingClasses().setIgnoreAllMissingClasses(ignoreMissingClasses);
        for (String packageInclude : packageIncludes) {
            options.getFilters().getIncludes().add(new JavadocLikePackageFilter(packageInclude));
        }
        for (String packageExclude : packageExcludes) {
            options.getFilters().getExcludes().add(new JavadocLikePackageFilter(packageExclude));
        }
        return options;
    }

    @Override
    public void run() {
        JarArchiveComparatorOptions comparatorOptions = createOptions();
        JarArchiveComparator jarArchiveComparator = new JarArchiveComparator(comparatorOptions);
        generateOutput(jarArchiveComparator);

    }

    private static String prettyPrint(List<JApiCmpArchive> archives) {
        StringBuilder sb = new StringBuilder();
        for (JApiCmpArchive archive : archives) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(archive.getFile().getName());
        }
        return sb.toString();
    }

    private static List<JApiCmpArchive> toJapiCmpArchives(List<Archive> archives) {
        List<JApiCmpArchive> out = new ArrayList<>(archives.size());
        for (Archive archive : archives) {
            out.add(archive.toJapicmpArchive());
        }
        return out;
    }

    private static String toClasspath(List<Archive> archives) {
        StringBuilder sb = new StringBuilder();
        for (Archive archive : archives) {
            if (sb.length() > 0) {
                sb.append(":");
            }
            sb.append(archive.file.getAbsolutePath());
        }
        return sb.toString();
    }

    private void generateOutput(JarArchiveComparator jarArchiveComparator) {
        // we create a dummy options because we don't want to avoid use of internal classes of JApicmp
        Options options = Options.newDefault();
        options.setOldClassPath(com.google.common.base.Optional.of(toClasspath(oldClasspath)));
        options.setNewClassPath(com.google.common.base.Optional.of(toClasspath(newClasspath)));
        final List<JApiCmpArchive> baseline = toJapiCmpArchives(oldArchives);
        final List<JApiCmpArchive> current = toJapiCmpArchives(newArchives);
        List<JApiClass> jApiClasses = jarArchiveComparator.compare(baseline, current);
        options.setOutputOnlyModifications(onlyModified);
        options.setOutputOnlyBinaryIncompatibleModifications(onlyBinaryIncompatibleModified);
        options.setIncludeSynthetic(includeSynthetic);
        options.setAccessModifier(AccessModifier.valueOf(accessModifier.toUpperCase()));
        File reportFile = null;
        if (xmlOutputFile != null) {
            options.setXmlOutputFile(com.google.common.base.Optional.of(xmlOutputFile.getAbsolutePath()));
            reportFile = xmlOutputFile;
        }

        if (htmlOutputFile != null) {
            options.setHtmlOutputFile(com.google.common.base.Optional.of(htmlOutputFile.getAbsolutePath()));
            reportFile = htmlOutputFile;
        }

        if (xmlOutputFile != null || htmlOutputFile != null) {
            XmlOutputGeneratorOptions xmlOptions = new XmlOutputGeneratorOptions();
            XmlOutputGenerator xmlOutputGenerator = new XmlOutputGenerator(jApiClasses, options, xmlOptions);
            XmlOutput xmlOutput = xmlOutputGenerator.generate();
            XmlOutputGenerator.writeToFiles(options, xmlOutput);
        }

        if (txtOutputFile != null) {
            StdoutOutputGenerator stdoutOutputGenerator = new StdoutOutputGenerator(options, jApiClasses);
            String output = stdoutOutputGenerator.generate();
            try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(txtOutputFile), "utf-8")
            )) {
                writer.write(output);
            } catch (IOException ex) {
                throw new GradleException("Unable to write report", ex);
            }
            if (reportFile == null) {
                reportFile = txtOutputFile;
            }
        }

        boolean hasCustomViolations = false;
        if (richReport != null) {
            final List<String> includedClasses = richReport.getIncludedClasses();
            final List<String> excludedClasses = richReport.getExcludedClasses();
            ViolationsGenerator generator = new ViolationsGenerator(includedClasses, excludedClasses);
            List<ViolationRuleConfiguration> rules = richReport.getRules();
            for (ViolationRuleConfiguration configuration : rules) {
                Map<String, String> arguments = configuration.getArguments();
                Class<? extends ViolationRule> ruleClass = configuration.getRuleClass();
                try {
                    ViolationRule rule = arguments == null ? ruleClass.newInstance() : ruleClass.getConstructor(Map.class).newInstance(arguments);
                    if (configuration.getClass() == ViolationRuleConfiguration.class) {
                        generator.addRule(rule);
                    } else if (configuration.getClass() == StatusChangeViolationRuleConfiguration.class) {
                        generator.addRule(((StatusChangeViolationRuleConfiguration) configuration).getStatus(), rule);
                    } else if (configuration.getClass() == CompatibilityChangeViolationRuleConfiguration.class) {
                        generator.addRule(((CompatibilityChangeViolationRuleConfiguration) configuration).getChange(), rule);
                    }
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    throw new GradleException("Unable to instantiate rule", e);
                }
            }

            Map<String, List<Violation>> violations = generator.toViolations(jApiClasses);
            for (List<Violation> violationList : violations.values()) {
                for (Violation violation : violationList) {
                    if (violation.getSeverity().equals(Severity.error)) {
                        hasCustomViolations = true;
                        break;
                    }
                }
                if (hasCustomViolations) {
                    break;
                }
            }
            File path = richReport.getDestinationDir();
            if (path == null) {
                path = new File(buildDir, "reports");
            }

            try {
                reportFile = new File(path, richReport.getReportName());
                richReport.getRenderer().newInstance().render(reportFile, new RichReportData(richReport.getTitle(), richReport.getDescription(), violations));
            } catch (InstantiationException | IllegalAccessException e) {
                throw new GradleException("Unable to create renderer", e);
            }
        }


        if ((failOnModification && hasBreakingChange(jApiClasses)) || hasCustomViolations) {
            String reportLink;
            try {
                reportLink = reportFile != null ? new URI("file", "", reportFile.toURI().getPath(), null, null).toString() : null;
            } catch (URISyntaxException e) {
                reportLink = null;
            }
            StringBuilder message = new StringBuilder("Detected binary changes between ")
                    .append(prettyPrint(current))
                    .append(" and ")
                    .append(prettyPrint(baseline));
            if (reportLink != null) {
                message.append(". See failure report at ").append(reportLink);
            }
            throw new GradleException(message.toString());
        }
    }

    private static boolean hasBreakingChange(final List<JApiClass> jApiClasses) {
        for (JApiClass jApiClass : jApiClasses) {
            if (!jApiClass.isBinaryCompatible()) {
                return true;
            }
        }
        return false;
    }

    public static class Archive implements Serializable {
        private final File file;
        private final String version;

        public Archive(final File file, final String version) {
            this.file = file;
            this.version = version;
        }

        @Override
        public String toString() {
            return file.getName();
        }

        public JApiCmpArchive toJapicmpArchive() {
            return new JApiCmpArchive(file, version);
        }
    }
}
