plugins {
    id("java")
}

val isCiBuild = providers.environmentVariable("CI").isPresent

tasks.withType<Test>().configureEach {
    useJUnitPlatform()

    maxParallelForks = if (isCiBuild) {
        Runtime.getRuntime().availableProcessors()
    } else {
        // https://docs.gradle.org/8.0/userguide/performance.html#execute_tests_in_parallel
        (Runtime.getRuntime().availableProcessors() / 2).takeIf { it > 0 } ?: 1
    }
}

tasks.test {
    // Skip default test.
    onlyIf { false }
}

val allGradle = listOf("6.6", "7.3", "7.6", GradleVersion.current().version)
val testJdk = providers.gradleProperty("me.champeau.japicmp.javaToolchain.test")
    .getOrElse("8").toInt()

// https://docs.gradle.org/current/userguide/compatibility.html
allGradle.forEach { gradleVersion ->
    if (gradleVersion < "7.3" && testJdk >= 17) return@forEach
    if (gradleVersion < "8.5" && testJdk >= 21) return@forEach
    if (gradleVersion < "8.8" && testJdk >= 22) return@forEach

    val task = tasks.register<Test>("testJdk${testJdk}onGradle${gradleVersion}") {
        group = LifecycleBasePlugin.VERIFICATION_GROUP
        description = "Runs the test suite on JDK $testJdk and Gradle $gradleVersion"

        systemProperty("gradleVersion", gradleVersion)
        javaLauncher = javaToolchains.launcherFor {
            languageVersion = JavaLanguageVersion.of(testJdk)
        }

        classpath = tasks.test.get().classpath
        testClassesDirs = tasks.test.get().testClassesDirs
    }
    tasks.check {
        dependsOn(task)
    }
}
