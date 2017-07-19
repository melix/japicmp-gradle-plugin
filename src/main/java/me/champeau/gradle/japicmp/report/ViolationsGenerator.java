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

import japicmp.model.JApiChangeStatus;
import japicmp.model.JApiClass;
import japicmp.model.JApiCompatibility;
import japicmp.model.JApiCompatibilityChange;
import japicmp.model.JApiConstructor;
import japicmp.model.JApiField;
import japicmp.model.JApiHasChangeStatus;
import japicmp.model.JApiImplementedInterface;
import japicmp.model.JApiMethod;
import me.champeau.gradle.japicmp.report.stdrules.UnchangedMemberRule;
import me.champeau.gradle.japicmp.report.stdrules.BinaryIncompatibleRule;
import me.champeau.gradle.japicmp.report.stdrules.RecordSeenMembersSetup;
import me.champeau.gradle.japicmp.report.stdrules.SourceCompatibleRule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class ViolationsGenerator {
    private final List<Pattern> includePatterns;
    private final List<Pattern> excludePatterns;
    private final Map<JApiCompatibilityChange, List<ViolationRule>> apiCompatibilityRules = new HashMap<JApiCompatibilityChange, List<ViolationRule>>();
    private final Map<JApiChangeStatus, List<ViolationRule>> statusRules = new HashMap<JApiChangeStatus, List<ViolationRule>>();
    private final List<ViolationRule> genericRules = new ArrayList<ViolationRule>();

    private final List<SetupRule> setupRules = new ArrayList<>();
    private final List<PostProcessViolationsRule> postProcessRules = new ArrayList<>();

    public ViolationsGenerator(final List<String> includePatterns, final List<String> excludePatterns) {
        this.includePatterns = toPatterns(includePatterns);
        this.excludePatterns = toPatterns(excludePatterns);
    }

    private static List<Pattern> toPatterns(List<String> regexps) {
        if (regexps == null) {
            return null;
        }
        List<Pattern> patterns = new ArrayList<>(regexps.size());
        for (String regexp : regexps) {
            patterns.add(Pattern.compile(regexp));
        }
        return patterns;
    }

    private static boolean anyMatches(List<Pattern> patterns, String className) {
        for (Pattern pattern : patterns) {
            if (pattern.matcher(className).find()) {
                return true;
            }
        }
        return false;
    }

    public void addRule(SetupRule rule) {
        setupRules.add(rule);
    }

    public void addRule(PostProcessViolationsRule rule) {
        postProcessRules.add(rule);
    }

    public void addRule(ViolationRule rule) {
        genericRules.add(rule);
    }

    public void addRule(JApiCompatibilityChange change, ViolationRule rule) {
        List<ViolationRule> violationRules = apiCompatibilityRules.get(change);
        if (violationRules == null) {
            violationRules = new ArrayList<>();
            apiCompatibilityRules.put(change, violationRules);
        }
        violationRules.add(rule);
    }

    public void addRule(JApiChangeStatus status, ViolationRule rule) {
        List<ViolationRule> violationRules = statusRules.get(status);
        if (violationRules == null) {
            violationRules = new ArrayList<>();
            statusRules.put(status, violationRules);
        }
        violationRules.add(rule);
    }

    public Map<String, List<Violation>> toViolations(List<JApiClass> classes) {
        addDefaultRuleIfNotConfigured();
        Context ctx = new Context();
        for (SetupRule setupRule : setupRules) {
            setupRule.execute(ctx);
        }
        injectContextIntoRules(ctx);
        for (JApiClass aClass : classes) {
            maybeProcess(aClass, ctx);
        }
        for (PostProcessViolationsRule postProcessViolationsRule : postProcessRules) {
            postProcessViolationsRule.execute(ctx);
        }
        return ctx.violations;
    }

    private void addDefaultRuleIfNotConfigured() {
        if (setupRules.isEmpty()
                && postProcessRules.isEmpty()
                && apiCompatibilityRules.isEmpty()
                && statusRules.isEmpty()
                && genericRules.isEmpty()) {
            addDefaultRules();
        }
    }

    public void addDefaultRules() {
        setupRules.add(new RecordSeenMembersSetup());
        addRule(JApiChangeStatus.NEW, new SourceCompatibleRule(Severity.info, "has been added in source compatible way"));
        addRule(JApiChangeStatus.MODIFIED, new SourceCompatibleRule(Severity.info, "has been modified in source compatible way"));
        addRule(JApiChangeStatus.UNCHANGED, new UnchangedMemberRule());
        genericRules.add(new BinaryIncompatibleRule());
        genericRules.add(new SourceCompatibleRule());
    }

    private void injectContextIntoRules(final ViolationCheckContext context) {
        for (List<ViolationRule> rules : apiCompatibilityRules.values()) {
            injectContextIntoRules(context, rules);
        }
        for (List<ViolationRule> rules : statusRules.values()) {
            injectContextIntoRules(context, rules);
        }
        injectContextIntoRules(context, genericRules);
    }

    private void injectContextIntoRules(final ViolationCheckContext context, final List<ViolationRule> rules) {
        for (ViolationRule rule : rules) {
            if (rule instanceof AbstractContextAwareViolationRule) {
                ((AbstractContextAwareViolationRule) rule).setContext(context);
            }
        }
    }

    private void maybeProcess(JApiClass clazz, Context context) {
        String fullyQualifiedName = clazz.getFullyQualifiedName();
        if (includePatterns != null) {
            if (anyMatches(includePatterns, fullyQualifiedName)) {
                if (excludePatterns != null && anyMatches(excludePatterns, fullyQualifiedName)) {
                    return;
                }
                processClass(clazz, context);
                return;
            } else {
                return;
            }
        } else if (excludePatterns != null) {
            if (anyMatches(excludePatterns, fullyQualifiedName)) {
                return;
            }
        }
        processClass(clazz, context);
    }

    private void processCompatibilityChange(JApiCompatibilityChange kind, JApiCompatibility member, final Context context) {
        List<ViolationRule> violationRules = apiCompatibilityRules.get(kind);
        if (violationRules != null) {
            for (ViolationRule violationRule : violationRules) {
                context.maybeAddViolation(violationRule.maybeViolation(member));
            }
        }
    }

    private void processClass(final JApiClass clazz, final Context context) {
        String oldClass = context.currentClass;
        try {
            context.currentClass = clazz.getFullyQualifiedName();
            processAllChanges(clazz, context);
            for (JApiField field : clazz.getFields()) {
                processAllChanges(field, context);
            }
            for (JApiMethod method : clazz.getMethods()) {
                processAllChanges(method, context);
            }
            for (JApiConstructor constructor : clazz.getConstructors()) {
                processAllChanges(constructor, context);
            }
            for (JApiImplementedInterface anInterface : clazz.getInterfaces()) {
                processAllChanges(anInterface, context);
            }
        } finally {
            context.currentClass = oldClass;
        }
    }

    private void processAllChanges(final JApiCompatibility elt, final Context context) {
        processStatusChanges(elt, context);
        processCompatibilityChanges(elt, context);
        processGenericRules(elt, context);
    }

    private void processCompatibilityChanges(final JApiCompatibility elt, final Context context) {
        for (JApiCompatibilityChange compatibilityChange : elt.getCompatibilityChanges()) {
            processCompatibilityChange(compatibilityChange, elt, context);
        }
    }

    private void processStatusChanges(final JApiCompatibility elt, final Context context) {
        if (elt instanceof JApiHasChangeStatus) {
            List<ViolationRule> violationRules = statusRules.get(((JApiHasChangeStatus) elt).getChangeStatus());
            if (violationRules != null) {
                for (ViolationRule violationRule : violationRules) {
                    context.maybeAddViolation(violationRule.maybeViolation(elt));
                }
            }
        }
    }

    private void processGenericRules(final JApiCompatibility elt, final Context context) {
        for (ViolationRule genericRule : genericRules) {
            context.maybeAddViolation(genericRule.maybeViolation(elt));
        }
    }

    private static class Context implements ViolationCheckContextWithViolations {
        // violations per fully-qualified class name
        private final Map<String, List<Violation>> violations = new LinkedHashMap<String, List<Violation>>();
        private final Map<String, Object> userData = new LinkedHashMap<>();

        private String currentClass;

        void maybeAddViolation(Violation v) {
            if (v == null) {
                return;
            }
            if (currentClass == null) {
                throw new IllegalStateException();
            }
            List<Violation> violations = this.violations.get(currentClass);
            if (violations == null) {
                violations = new ArrayList<Violation>();
                this.violations.put(currentClass, violations);
            }
            violations.add(v);
        }

        @Override
        public String getClassName() {
            return currentClass;
        }

        @Override
        public Map<String, ?> getUserData() {
            return userData;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getUserData(final String key) {
            return (T) userData.get(key);
        }

        @Override
        public <T> void putUserData(final String key, final T value) {
            userData.put(key, value);
        }

        @Override
        public Map<String, List<Violation>> getViolations() {
            return violations;
        }
    }
}
