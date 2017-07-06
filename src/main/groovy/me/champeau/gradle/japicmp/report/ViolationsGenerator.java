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
import org.gradle.api.Transformer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ViolationsGenerator {
    private Transformer<Boolean, String> classFilter;
    private final Map<JApiCompatibilityChange, List<ViolationRule>> apiCompatibilityRules = new HashMap<JApiCompatibilityChange, List<ViolationRule>>();
    private final Map<JApiChangeStatus, List<ViolationRule>> statusRules = new HashMap<JApiChangeStatus, List<ViolationRule>>();
    private final List<ViolationRule> genericRules = new ArrayList<ViolationRule>();

    public void setClassFilter(final Transformer<Boolean, String> classFilter) {
        this.classFilter = classFilter;
    }

    public void addRule(ViolationRule rule) {
        genericRules.add(rule);
    }

    public void addRule(JApiCompatibilityChange change, ViolationRule rule) {
        List<ViolationRule> violationRules = apiCompatibilityRules.get(change);
        if (violationRules == null) {
            violationRules = new ArrayList<ViolationRule>();
            apiCompatibilityRules.put(change, violationRules);
        }
        violationRules.add(rule);
    }

    public void addRule(JApiChangeStatus status, ViolationRule rule) {
        List<ViolationRule> violationRules = statusRules.get(status);
        if (violationRules == null) {
            violationRules = new ArrayList<ViolationRule>();
            statusRules.put(status, violationRules);
        }
        violationRules.add(rule);
    }

    public Map<String, List<Violation>> toViolations(List<JApiClass> classes) {
        Context ctx = new Context();
        for (JApiClass aClass : classes) {
            maybeProcess(aClass, ctx);
        }
        return ctx.violations;
    }

    private void maybeProcess(JApiClass clazz, Context context) {
        if (classFilter == null || classFilter.transform(clazz.getFullyQualifiedName())) {
            processClass(clazz, context);
        }
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
    }

    private void processAllChanges(final JApiCompatibility elt, final Context context) {
        processElement(elt, context);
        for (JApiCompatibilityChange compatibilityChange : elt.getCompatibilityChanges()) {
            processCompatibilityChange(compatibilityChange, elt, context);
        }
    }

    private void processElement(final JApiCompatibility elt, final Context context) {
        for (ViolationRule genericRule : genericRules) {
            context.maybeAddViolation(genericRule.maybeViolation(elt));
        }
        if (elt instanceof JApiHasChangeStatus) {
            List<ViolationRule> violationRules = statusRules.get(((JApiHasChangeStatus) elt).getChangeStatus());
            if (violationRules != null) {
                for (ViolationRule violationRule : violationRules) {
                    context.maybeAddViolation(violationRule.maybeViolation(elt));
                }
            }
        }
    }

    private static class Context {
        // violations per fully-qualified class name
        private final Map<String, List<Violation>> violations = new LinkedHashMap<String, List<Violation>>();

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
    }
}
