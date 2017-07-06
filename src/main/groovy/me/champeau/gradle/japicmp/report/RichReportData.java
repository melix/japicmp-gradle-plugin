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
package me.champeau.gradle.japicmp.report;

import java.util.List;
import java.util.Map;

public class RichReportData {
    private final String reportTitle;
    private final String description;
    private final Map<String, List<Violation>> violations;

    public RichReportData(final String reportTitle, final String description, final Map<String, List<Violation>> violations) {
        this.reportTitle = reportTitle;
        this.description = description;
        this.violations = violations;
    }

    public String getReportTitle() {
        return reportTitle;
    }

    public String getDescription() {
        return description;
    }

    public Map<String, List<Violation>> getViolations() {
        return violations;
    }
}
