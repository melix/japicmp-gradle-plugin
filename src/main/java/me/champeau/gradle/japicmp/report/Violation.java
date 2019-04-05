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

import japicmp.util.Optional;
import japicmp.model.JApiAnnotation;
import japicmp.model.JApiAnnotationElement;
import japicmp.model.JApiClass;
import japicmp.model.JApiCompatibility;
import japicmp.model.JApiCompatibilityChange;
import japicmp.model.JApiConstructor;
import japicmp.model.JApiField;
import japicmp.model.JApiImplementedInterface;
import japicmp.model.JApiMethod;
import japicmp.model.JApiSuperclass;
import javassist.CtConstructor;
import javassist.CtMethod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Violation {
    private final JApiCompatibility member;
    private final Severity severity;
    private final String humanExplanation;

    private Violation(final JApiCompatibility member, final Severity severity, final String humanExplanation) {
        this.member = member;
        this.severity = severity;
        this.humanExplanation = humanExplanation;
    }

    public static Violation notBinaryCompatible(JApiCompatibility member) {
        return notBinaryCompatible(member, Severity.error);
    }

    public static Violation notBinaryCompatible(JApiCompatibility member, Severity severity) {
        return new Violation(member, severity, "Is not binary compatible");
    }

    public static Violation error(JApiCompatibility member, String explanation) {
        return new Violation(member, Severity.error, explanation);
    }

    public static Violation info(JApiCompatibility member, String explanation) {
        return new Violation(member, Severity.info, explanation);
    }

    public static Violation warning(JApiCompatibility member, String explanation) {
        return new Violation(member, Severity.warning, explanation);
    }

    public static Violation accept(JApiCompatibility member, String explanation) {
        return new Violation(member, Severity.accepted, explanation);
    }

    public static Violation any(JApiCompatibility member, Severity severity, String explanation) {
        return new Violation(member, severity, explanation);
    }

    public JApiCompatibility getMember() {
        return member;
    }

    public Severity getSeverity() {
        return severity;
    }

    public String getHumanExplanation() {
        return humanExplanation;
    }

    public List<String> getChanges() {
        List<JApiCompatibilityChange> changes = member.getCompatibilityChanges();
        if (changes.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> describedChanges = new ArrayList<String>(changes.size());
        for (JApiCompatibilityChange change : changes) {
            describedChanges.add(describe(change));
        }
        return describedChanges;
    }

    @Override
    public String toString() {
        return describe(member) + ": " + humanExplanation;
    }

    public static String describe(JApiCompatibility member) {
        if (member instanceof JApiAnnotation) {
            return "Annotation " + ((JApiAnnotation) member).getFullyQualifiedName();
        }
        if (member instanceof JApiAnnotationElement) {
            return "Annotation element " + ((JApiAnnotationElement) member).getName();
        }
        if (member instanceof JApiConstructor) {
            JApiConstructor method = (JApiConstructor) member;
            Optional<CtConstructor> changedMethod = method.getNewConstructor();
            if (!changedMethod.isPresent()) {
                changedMethod = method.getOldConstructor();
            }
            return "Constructor " + (changedMethod.isPresent() ? changedMethod.get().getLongName() : method.getName());
        }
        if (member instanceof JApiMethod) {
            JApiMethod method = (JApiMethod) member;
            Optional<CtMethod> changedMethod = method.getNewMethod();
            if (!changedMethod.isPresent()) {
                changedMethod = method.getOldMethod();
            }
            return "Method " + (changedMethod.isPresent() ? changedMethod.get().getLongName() : method.getName());
        }
        if (member instanceof JApiField) {
            return "Field " + ((JApiField) member).getName();
        }
        if (member instanceof JApiClass) {
            return "Class " + ((JApiClass) member).getFullyQualifiedName();
        }
        if (member instanceof JApiSuperclass) {
            Optional<JApiClass> jApiClass = ((JApiSuperclass) member).getJApiClass();
            return "Superclass " + (jApiClass.isPresent()?jApiClass.get():"[removed]");
        }
        if (member instanceof JApiImplementedInterface) {
            return "Implemented interface " + ((JApiImplementedInterface) member).getFullyQualifiedName();
        }
        return member.toString();
    }

    public static String describe(JApiCompatibilityChange change) {
        switch (change) {
            case CLASS_REMOVED:
                return "Class has been removed";
            case CLASS_NOW_ABSTRACT:
                return "Class is now abstract";
            case CLASS_NOW_FINAL:
                return "Class is now final";
            case CLASS_NO_LONGER_PUBLIC:
                return "Class is no longer public";
            case CLASS_TYPE_CHANGED:
                return "Class type changed";
            case CLASS_NOW_CHECKED_EXCEPTION:
                return "Exception is now a checked exception";
            case SUPERCLASS_REMOVED:
                return "Superclass has been removed";
            case SUPERCLASS_ADDED:
                return "Superclass has been added";
            case SUPERCLASS_MODIFIED_INCOMPATIBLE:
                return "Superclass has been changed in an incompatible way";
            case INTERFACE_ADDED:
                return "Interface has been added";
            case INTERFACE_REMOVED:
                return "Interface has been removed";
            case METHOD_REMOVED:
                return "Method has been removed";
            case METHOD_REMOVED_IN_SUPERCLASS:
                return "Method has been removed in superclass";
            case METHOD_LESS_ACCESSIBLE:
                return "Method is less accessible";
            case METHOD_LESS_ACCESSIBLE_THAN_IN_SUPERCLASS:
                return "Method is less accessible than in superclass";
            case METHOD_IS_STATIC_AND_OVERRIDES_NOT_STATIC:
                return "Method is static but overrides a non-static one";
            case METHOD_RETURN_TYPE_CHANGED:
                return "Method return type has changed";
            case METHOD_NOW_ABSTRACT:
                return "Method is now abstract";
            case METHOD_NOW_FINAL:
                return "Method is now final";
            case METHOD_NOW_STATIC:
                return "Method is now static";
            case METHOD_NO_LONGER_STATIC:
                return "Method is no longer static";
            case METHOD_ADDED_TO_INTERFACE:
                return "Method added to interface";
            case METHOD_NOW_THROWS_CHECKED_EXCEPTION:
                return "Method is now throws a checked exception";
            case METHOD_ABSTRACT_ADDED_TO_CLASS:
                return "Abstract method has been added to this class";
            case METHOD_ABSTRACT_ADDED_IN_SUPERCLASS:
                return "Abstract method has been added to a superclass";
            case METHOD_ABSTRACT_ADDED_IN_IMPLEMENTED_INTERFACE:
                return "Abstract method has been added in implemented interface";
            case FIELD_STATIC_AND_OVERRIDES_STATIC:
                return "Field is static and overrides another static field";
            case FIELD_LESS_ACCESSIBLE_THAN_IN_SUPERCLASS:
                return "Field is less accessible than in superclass";
            case FIELD_NOW_FINAL:
                return "Field is now final";
            case FIELD_NOW_STATIC:
                return "Field is now static";
            case FIELD_NO_LONGER_STATIC:
                return "Field is no longer static";
            case FIELD_TYPE_CHANGED:
                return "Field type has changed";
            case FIELD_REMOVED:
                return "Field has been removed";
            case FIELD_REMOVED_IN_SUPERCLASS:
                return "Field has been removed in superclass";
            case FIELD_LESS_ACCESSIBLE:
                return "Field is less accessible";
            case CONSTRUCTOR_REMOVED:
                return "Constructor has been removed";
            case CONSTRUCTOR_LESS_ACCESSIBLE:
                return "Constructor is less accessible";
        }
        return change.toString();
    }
}
