plugins {
    alias(libs.plugins.buildscanRecipes)
    alias(libs.plugins.pluginPublish)
    id("me.champeau.convention")
    id("me.champeau.convention-testing")
}

buildScanRecipes {
    recipes("git-status")
    recipe(mapOf("baseUrl" to "https://github.com/melix/japicmp-gradle-plugin/tree"), "git-commit")
}

dependencies {
    api(libs.japicmp) {
        exclude(group = "com.google.guava")
        exclude(group = "io.airlift")
        exclude(group = "javax.xml.bind")
        exclude(group = "com.sun.xml.bind")
    }
    compileOnly(localGroovy())
    compileOnly(gradleApi())

    testImplementation(gradleTestKit())
    testImplementation(libs.spock)
}
