plugins {
    id("java")
}

val isCiBuild = providers.environmentVariable("CI").isPresent

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    inputs.dir("src/test/test-projects").withPropertyName("testProjects")

    maxParallelForks = if (isCiBuild) {
        Runtime.getRuntime().availableProcessors()
    } else {
        // https://docs.gradle.org/8.8/userguide/performance.html#execute_tests_in_parallel
        (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
    }
}

tasks.test {
    // Skip default test.
    onlyIf { false }
}

// Test on older Gradle and the latest one.
val allGradle = listOf(
    GradleVersion.version("6.6"),
    GradleVersion.version("7.3"),
    GradleVersion.version("7.6"),
    GradleVersion.current(),
)
// Configure various JDKs on CI.
val testJdk = providers.gradleProperty("me.champeau.japicmp.javaToolchain.test")
    .getOrElse("8").toInt()

// https://docs.gradle.org/current/userguide/compatibility.html
allGradle.forEach { gv ->
    // Gradle 7.3+ supports Java 17.
    if (gv < GradleVersion.version("7.3") && testJdk >= 17) return@forEach
    // Gradle 8.5+ supports Java 21.
    if (gv < GradleVersion.version("8.5") && testJdk >= 21) return@forEach
    // Gradle 8.8+ supports Java 22.
    if (gv < GradleVersion.version("8.8") && testJdk >= 22) return@forEach
    // Executing Gradle on JVM versions 16 and lower has been deprecated. Use JVM 17 or greater to execute Gradle.
    // See https://docs.gradle.org/8.10/userguide/upgrading_version_8.html#minimum_daemon_jvm_version.
    if (gv >= GradleVersion.version("8.10") && testJdk < 17) return@forEach

    val gradleVersion = gv.version

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
