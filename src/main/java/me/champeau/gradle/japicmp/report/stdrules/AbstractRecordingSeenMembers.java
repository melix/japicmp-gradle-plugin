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
import me.champeau.gradle.japicmp.report.AbstractContextAwareViolationRule;
import me.champeau.gradle.japicmp.report.Violation;

import java.util.Set;

/**
 * A rule which allows memoizing which members have already reported an error, so that we don't
 * add them multiple times. This rule needs to be setup with {@link RecordSeenMembersSetup}
 */
public abstract class AbstractRecordingSeenMembers extends AbstractContextAwareViolationRule {
    @Override
    public final Violation maybeViolation(final JApiCompatibility member) {
        Set<JApiCompatibility> seen = getContext().getUserData(RecordSeenMembersSetup.SEEN);
        if (seen == null) {
            throw new IllegalStateException(
                    "The " + this.getClass().getSimpleName() + " rule cannot be used if the " +
                    RecordSeenMembersSetup.class.getSimpleName() + " setup rule hasn't been added.");
        }
        if (!seen.contains(member)) {
            Violation violation = maybeAddViolation(member);
            if (violation != null) {
                seen.add(member);
                return violation;
            }
        }
        return null;
    }

    protected abstract Violation maybeAddViolation(final JApiCompatibility member);
}
