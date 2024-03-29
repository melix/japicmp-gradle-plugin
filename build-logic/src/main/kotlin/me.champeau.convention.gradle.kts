plugins {
    id("groovy-gradle-plugin")
    id("maven-publish")
    id("signing")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(8)
    }
    withSourcesJar()
    withJavadocJar()
}

tasks.named("test") {
    inputs.dir("src/test/test-projects").withPropertyName("testProjects")
}

gradlePlugin {
    website = "https://github.com/melix/japicmp-gradle-plugin"
    vcsUrl = "https://github.com/melix/japicmp-gradle-plugin"

    plugins {
        create("japicmpPlugin") {
            id = "me.champeau.gradle.japicmp"
            implementationClass = "me.champeau.gradle.japicmp.JapicmpPlugin"
            displayName = "Gradle Plugin for JApicmp"
            description = "Gradle Plugin for JApicmp"
            tags = listOf("jacpicmp")
        }
    }
}

publishing {
    repositories {
        maven {
            name = "build"
            setUrl(layout.buildDirectory.file("repo").get().asFile.path)
        }
    }
}

signing {
    useGpgCmd()
    publishing.publications.forEach { pub ->
        sign(pub)
    }
}
