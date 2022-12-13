description = "Http4k HTTP Server built on top of Helidon Nima"

dependencies {
    api(project(":http4k-core"))
    api("io.helidon.nima.webserver:helidon-nima-webserver:_")
    testImplementation(project(path = ":http4k-core", configuration = "testArtifacts"))
    testRuntimeOnly("org.junit.platform:junit-platform-console-standalone:1.9.1")
}

tasks {
    test {
        dependsOn("runTests")
        exclude("**/*")
    }

    task("runTests", JavaExec::class) {
        main = "org.junit.platform.console.ConsoleLauncher"
        classpath = sourceSets["test"].runtimeClasspath
        args = listOf("--scan-class-path")
        jvmArgs = listOf("--enable-preview")
    }
}
