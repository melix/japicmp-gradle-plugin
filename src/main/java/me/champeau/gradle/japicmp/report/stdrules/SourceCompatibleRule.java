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
package me.champeau.gradle.japicmp.report.stdrules;

import japicmp.model.JApiCompatibility;
import me.champeau.gradle.japicmp.report.Severity;
import me.champeau.gradle.japicmp.report.Violation;

public class SourceCompatibleRule extends AbstractRecordingSeenMembers {
    private final Severity severity;
    private final String message;

    public SourceCompatibleRule(final Severity severity, String message) {
        this.severity = severity;
        this.message = message;
    }

    public SourceCompatibleRule() {
        this(Severity.warning, "is source compatible");
    }

    @Override
    public Violation maybeAddViolation(final JApiCompatibility member) {
        if (member.isSourceCompatible()) {
            return Violation.any(member, severity, message);
        }
        return null;
    }
}
