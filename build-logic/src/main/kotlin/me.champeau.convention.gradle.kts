plugins {
    `java-gradle-plugin`
    id("com.gradle.plugin-publish")
    id("com.vanniktech.maven.publish")
}

version = providers.gradleProperty("VERSION_NAME").get()
group = providers.gradleProperty("GROUP").get()
description = providers.gradleProperty("POM_DESCRIPTION").get()

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(8)
    }
    withSourcesJar()
    withJavadocJar()
}

tasks.test {
    inputs.dir("src/test/test-projects").withPropertyName("testProjects")
}

gradlePlugin {
    website = providers.gradleProperty("POM_URL")
    vcsUrl = providers.gradleProperty("POM_URL")

    plugins {
        create("japicmpPlugin") {
            id = "me.champeau.gradle.japicmp"
            implementationClass = "me.champeau.gradle.japicmp.JapicmpPlugin"
            displayName = providers.gradleProperty("POM_NAME").get()
            description = providers.gradleProperty("POM_DESCRIPTION").get()
            tags = listOf("jacpicmp")
        }
    }
}
