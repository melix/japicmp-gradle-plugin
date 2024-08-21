plugins {
    id("me.champeau.buildscan-recipes") version "0.2.3"
    id("com.gradle.plugin-publish") version "1.2.1"
    id("me.champeau.convention")
    id("me.champeau.convention-testing")
}

buildScanRecipes {
    recipes("git-status")
    recipe(mapOf("baseUrl" to "https://github.com/melix/japicmp-gradle-plugin/tree"), "git-commit")
}

dependencies {
    api("com.github.siom79.japicmp:japicmp:0.23.0") {
        exclude(group = "com.google.guava")
        exclude(group = "io.airlift")
        exclude(group = "javax.xml.bind")
        exclude(group = "com.sun.xml.bind")
    }
    compileOnly(localGroovy())
    compileOnly(gradleApi())

    testImplementation(gradleTestKit())
    testImplementation("org.spockframework:spock-core:2.3-groovy-3.0")
}
