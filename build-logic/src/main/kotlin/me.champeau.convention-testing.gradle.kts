plugins {
    id("java")
}

val wrapperVersion: String = GradleVersion.current().version

// The plugin is broken with Gradle 5.6.*
val otherVersions = listOf("6.9.1", "7.2", "7.3", "7.4")

val testedGradleVersions = otherVersions - wrapperVersion

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
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
