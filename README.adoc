= JApicmp Gradle Plugin
:japicmp-url: https://github.com/siom79/japicmp
:issues: https://github.com/melix/japicmp-gradle-plugin/issues
:gradle-url: http://gradle.org/
:plugin-version: 0.4.1

image:https://github.com/melix/japicmp-gradle-plugin/actions/workflows/gradle-build.yml/badge.svg["Build Status", link="https://github.com/melix/japicmp-gradle-plugin/actions/workflows/gradle-build.yml"]
image:http://img.shields.io/badge/license-ASF2-blue.svg["Apache License 2", link="http://www.apache.org/licenses/LICENSE-2.0.txt"]

The japicmp-gradle-plugin provides binary compatibility reporting through {japicmp-url}[JApicmp] using {gradle-url}[Gradle].

== Installation

This plugin requires Gradle 6+. Use the following snippet inside a Gradle build file:

[source,groovy]
[subs="attributes"]
----
plugins {
    id 'me.champeau.gradle.japicmp' version '{plugin-version}'
}
----

or (not recommended):

[source,groovy]
[subs="attributes"]
.build.gradle
----
buildscript {
    repositories {
        mavenCentral()
    }

    dependencies {
        classpath 'me.champeau.gradle:japicmp-gradle-plugin:{plugin-version}'
    }
}
apply plugin: 'me.champeau.gradle.japicmp'
----

== Configuration

The plugin provides a new task type: `me.champeau.gradle.japicmp.JapicmpTask` that you can use to compare two jars. This task
exposes the following properties as part of its configuration:

[horizontal]
oldClasspath:: The classpath of the baseline library to compare. Type: _FileCollection_
newClasspath:: The classpath of the current version of the library, which you want to check binary compatibility  Type: _FileCollection_
oldArchives:: The jar files which will be used as the baseline for comparison. Type: _FileCollection_.
newArchives:: The jar files we want to analyze. Type: Type: _FileCollection_.
onlyModified:: Outputs only modified classes/methods. If not set to true, all classes and methods are printed. Type: _boolean_. Default value: _false_
onlyBinaryIncompatibleModified:: Outputs only classes/methods with modifications that result in binary incompatibility. Type: _boolean_. Default value: _false_
packageIncludes:: List of package names to include, * can be used as wildcard. Type: _List<String>_
packageExcludes:: List of package names to exclude, * can be used as wildcard. Type: _List<String>_
classIncludes:: List of classes to include. Type: _List<String>_
classExcludes:: List of classes to exclude. Type: _List<String>_
methodIncludes:: List of methods to include. Type: _List<String>_
methodExcludes:: List of methods to exclude. Type: _List<String>_
fieldIncludes:: List of fields to include. Type: _List<String>_
fieldExcludes:: List of fields to exclude. Type: _List<String>_
annotationIncludes:: List of annotations to include. The string must begin with '@'. Type: _List<String>_
annotationExcludes:: List of annotations to exclude. The string must begin with '@'. Type: _List<String>_
compatibilityChangeExcludes:: List of compatibility changes to exclude, marking them as source and binary compatible. The string must match a value of the `japicmp.model.JApiCompatibilityChange` enum. Type: _List<String>_
accessModifier:: Sets the access modifier level (public, package, protected, private). Type: _String_. Default value: _public_
failOnSourceIncompatibility:: Fails if the changes result in source level incompatibility. Setting this to `true` also implicitly enables `failOnModification`. imType: _boolean_. Default value: _false_
failOnModification:: When set to true, the build fails in case a modification has been detected. Type: _boolean_. Default value: _false_
xmlOutputFile:: Path to the generated XML report. Type: _File_. Default value: _null_
htmlOutputFile:: Path to the generated HTML report. Type: _File_. Default value: _null_
txtOutputFile:: Path to the generated TXT report. Type: _File_. Default value: _null_
includeSynthetic:: Synthetic classes and class members (like e.g. bridge methods) are not tracked per default. This new option enables the tracking of such kind of classes and class members
ignoreMissingClasses:: Ignores all superclasses or interfaces that missing on the classpath. Default value: _false_

If you don't set _oldArchives_ and _newArchives_, the plugin will infer them from the _oldClasspath_ and _newClasspath_ properties:

   * if you set the classpath to a configuration, the archives to compare will be the first level dependencies of that configuration
   * if you set the classpath to a simple file collection, all archives will be compared

== Usage

Add the following to your build file:

[source,groovy]
----
tasks.register("japicmp", me.champeau.gradle.japicmp.JapicmpTask) {
    oldClasspath.from(files('path/to/reference.jar'))
    newClasspath.from(tasks.named('jar'))
    onlyModified = true
    failOnModification = true
    txtOutputFile = layout.buildDirectory.file("reports/japi.txt")
}
----

== JApiCompatibilityChange filtering

The plugin supports simple exclusion for identified compatibility changes, turning these into binary and source
compatible during API comparison:

[source,groovy]
----
tasks.register("japicmp", me.champeau.gradle.japicmp.JapicmpTask) {
   ...
   compatibilityChangeExcludes = [ "METHOD_NEW_DEFAULT" ]
}
----

The `JApiCompatibilityChange` enum from japicmp represents the list of identified compatibility changes which
can be excluded. For simplicity, the plugin is configured with a _List<String>_ instead.

== Custom filtering

The plugin supports adding filters for bytecode members before they are considered for API comparison:

[source,groovy]
----
tasks.register("japicmp", me.champeau.gradle.japicmp.JapicmpTask) {
   ...
   addIncludeFilter(MyCustomFilter)
   addExcludeFilter(MyOtherFilter)
}
----

where `MyIncludeFilter` and `MyExcludeFilter` are classes implementing types inheriting from `japicmp.filter.Filter`.

For example, adding the following filter as an exclude filter will hide fields that are annotated with `@Custom` or have a name that contains `Custom` from the API comparison:

[source,groovy]
----
class MyOtherFilter implements FieldFilter {
    @Override
    boolean matches(CtField field) {
        return field.hasAnnotation("Custom") || field.name.contains("Custom")
    }
}
----

== Custom reports and failure conditions

The plugin supports a DSL to generate custom reports based on the API comparison result. This has several advantages:

* you can generate a report that focuses only on your public API, leaving the internal APIs out
* you can implement custom rules to determine if the build should fail or not
* the report can be presented to users and provide guidance for migration from one version to the other

=== Configuration

The report can be configured using the `richReport` block:

[source,groovy]
----
tasks.register("japicmp", me.champeau.gradle.japicmp.JapicmpTask) {
   ...
   richReport {
      ...
   }
}
----

Options for the rich report are:

[horizontal]
renderer:: The renderer used to generate the report. By default, it uses the GroovyReportRenderer
includedClasses:: A list of strings representing inclusion patterns (interpreted as regular expressions). Only classes matching this pattern will be included.
excludedClasses:: A list of strings representing exclusion patterns. If a class fully qualified name matches any of those patterns, it will not be included.
destinationDir:: the directory where to store the report
reportName:: file name of the generated report (defaults to `rich-report.html`)
title:: a title for the report
description:: a description for the report
addDefaultRules:: a boolean, indicating whether the default rules should be added or not.

If no rules are explicitly defined, the default rules are applied. If any rule is added, the default rules won't be applied _unless_ `addDefaultRules` is set to `true`.

=== Custom rules

Rules are used to add violations to the report. The "violation" term must be taken in a simple sense, as it represents data
to be shown in the report, whether it's a critical violation or just information.

A violation consists of a triplet (member, severity, explanation), that will be seen in the report. For example, if a binary
incompatibility is found, you can create a violation using:

```
Violation.notBinaryCompatible(member)
```

which will automatically assign it to the `error` severity, leading in a build failure. However, it is possible to create any
kind of violation, and even accept binary incompatible changes.

Rules can be applied to 3 different levels:

* all members (a generic rule applied unconditionnaly)
* on specific change types (`NEW`, `REMOVED`, `UNCHANGED`, `MODIFIED`), see `JApiChangeStatus`
* on specific compatibility change descriptors (see `JApiCompatibilityChange`)

Rules are executed in the following order:

. status change first
. specific compatibility change
. generic rules

For example, imagine that we want to check that all new methods are annotated with `@Incubating` (this is a rule in the Gradle project).
Then, you need to create a rule class which will implement that check:

[source,groovy]
----
class IncubatingMissingRule implements ViolationRule {
    @Override
    Violation maybeViolation(final JApiCompatibility member) {
        if (member instanceof JApiMethod) {
            if (!member.annotations.find { it.fullyQualifiedName == 'org.gradle.api.Incubating' }) {
                if (!member.jApiClass.annotations.find {
                    it.fullyQualifiedName == 'org.gradle.api.Incubating'
                }) {
                    Violation.error(member, "New method is not annotated with @Incubating")
                }
            }
        }
    }
}
----

and then you need to configure the report to use that rule:

[source,groovy]
----
richReport {
   addRule(JApiChangeStatus.NEW, IncubatingMissingRule)
}
----

Rules can take arguments, but those are limited to `Map<String, String>`. For example, the following rule will mark
a binary breaking change as an error, unless it is reviewed and accepted. The list of acceptations is passed as an
argument to the rule:

[source,groovy]
----
class AcceptedRegressionRule implements ViolationRule {
    private final Map<String, String> acceptedViolations

    public AcceptedRegressionRule(Map<String, String> params) {
        acceptedViolations = params
    }

    @Override
    Violation maybeViolation(final JApiCompatibility member) {
        if (!member.binaryCompatible) {
            def acceptation = acceptedViolations[Violation.describe(member)]
            if (acceptation) {
                Violation.accept(member, acceptation)
            } else {
                Violation.notBinaryCompatible(member)
            }
        }
    }
}
----

and here's how the rule is applied:

[source,groovy]
----
richReport {
   addRule(AcceptedRegressionRule, acceptedViolations)
}
----

=== Setup and post-process rules

Since release 0.2.2, the plugin also supports setup and post-process rules. Setup rules allow setting up some global
context that can be accessed by rules extending `AbstractContextAwareViolationRule`. This can be useful when you need
to share data between rules, and perform a final check in a post-process rule.

Setup rules need to implement `SetupRule`:

[source,groovy]
----
class MySetupRule implements SetupRule {

    @Override
    void execute(final ViolationCheckContext violationCheckContext) {
        // this is going to be executed before any other rule is executed
        violationCheckContext.userData.executed = false
    }
}
----

and declared using `addSetupRule`:


[source,groovy]
----
richReport {
   addSetupRule(MySetupRule)
}
----

Then the context can be accessed in rules implementing `AbstractContextAwareViolationRule`:

[source,groovy]
----
class ContextAwareRule extends AbstractContextAwareViolationRule {

    @Override
    Violation maybeViolation(final JApiCompatibility member) {
        // this rule is accessing the global context and can mutate user data
        context.userData.executed = true

        return null
    }
}
----

And then a post-process rule has access to the user data, and can also mutate the actual list of violations per class,
before the report is generated:

[source,groovy]
----
class MyTearDownRule implements PostProcessViolationsRule {

    @Override
    void execute(final ViolationCheckContextWithViolations violationCheckContextWithViolations) {
        // this rule is executed once all checks have been performed, just before the generation
        // of the report
        // it gives the opportunity to add additional violations, or filter them, or fail
        // with a custom error
        assert violationCheckContextWithViolations.userData.executed == true
        assert !violationCheckContextWithViolations.violations.isEmpty()
    }
}
----

It needs to be wired in using the `addPostProcessRule` hook:

[source,groovy]
----
richReport {
   addPostProcessRule(MySetupRule)
}
----

== Avoiding multiple violations for the same class

Since 0.2.5, it is now possible to track which members have already resulted in a violation.
Since rules are executed in order, and that you can have a rule applied for a status change and a generic rule applied on the same member, it was possible for a member to trigger multiple violations.
To avoid this, you can make your rule extend `AbstractRecordingSeenMembers`. This rule requires the `RecordSeenMembersSetup` to be applied, and it will only add a violation, if no other violation for the same member was added before.
