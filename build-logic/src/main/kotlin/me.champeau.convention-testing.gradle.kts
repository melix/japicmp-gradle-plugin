plugins {
    id("java")
}

val wrapperVersion: String = GradleVersion.current().version

// The plugin is broken with Gradle 5.6.*
val otherVersions = listOf("7.6.2")

val testedGradleVersions = otherVersions - wrapperVersion

tasks.withType<Test>().configureEach {
    useJUnitPlatform()

    val testJavaVersion = providers.gradleProperty("me.champeau.japicmp.javaToolchain.test")
        .getOrElse("8")
    javaLauncher.set(
        javaToolchains.launcherFor {
            languageVersion = JavaLanguageVersion.of(testJavaVersion)
        }
    )

    maxParallelForks = if (System.getenv("CI") != null) {
        Runtime.getRuntime().availableProcessors()
    } else {
        // https://docs.gradle.org/8.0/userguide/performance.html#execute_tests_in_parallel
        (Runtime.getRuntime().availableProcessors() / 2).takeIf { it > 0 } ?: 1
    }
}

tasks.test {
    description = "Runs the tests against Gradle $wrapperVersion"
    systemProperty("gradleVersion", wrapperVersion)
}

val testAllGradleVersions by tasks.registering {
    group = "verification"
    description = "Runs the tests against all supported Gradle versions"
    dependsOn(tasks.test)
}

tasks.check {
    dependsOn(testAllGradleVersions)
}

testedGradleVersions.forEach { gradleVersion ->
    val task = tasks.register<Test>("testGradle${gradleVersion.replace(".", "_").replace("-", "_")}") {
        group = "verification"
        description = "Runs the tests against Gradle $gradleVersion"
        classpath = tasks.test.get().classpath
        systemProperty("gradleVersion", gradleVersion)
    }
    testAllGradleVersions.configure {
        dependsOn(task)
    }
}
