description = "Http4k HTTP Server built on top of Helidon Nima"

dependencies {
    api(project(":http4k-core"))
    api("io.helidon.nima.webserver:helidon-nima-webserver:_")
    testImplementation(testFixtures(project(":http4k-core")))
    testRuntimeOnly("org.junit.platform:junit-platform-console-standalone:_")
}

tasks {
    test {
        dependsOn("runTests")
        exclude("**/*")
    }

    register<JavaExec>("runTests") {
        mainClass.set("org.junit.platform.console.ConsoleLauncher")
        classpath = sourceSets["test"].runtimeClasspath
        args = listOf("--scan-class-path")
        jvmArgs = listOf("--enable-preview")
    }
}
