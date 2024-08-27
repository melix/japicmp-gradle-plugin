plugins {
    `java-gradle-plugin`
    id("com.gradle.plugin-publish")
    id("com.vanniktech.maven.publish")
}

version = providers.gradleProperty("VERSION_NAME").get()
group = providers.gradleProperty("GROUP").get()
description = providers.gradleProperty("POM_DESCRIPTION").get()

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
    withSourcesJar()
    withJavadocJar()
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
