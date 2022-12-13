description = "Http4k HTTP Server built on top of Helidon Nima"

dependencies {
    api(project(":http4k-core"))
    api("io.helidon.nima.webserver:helidon-nima-webserver:_")
    testImplementation(project(path = ":http4k-core", configuration = "testArtifacts"))
    testRuntimeOnly("org.junit.platform:junit-platform-console-standalone:1.9.1")
}

task("runSingleTest", JavaExec::class) {
    main = "org.junit.platform.console.ConsoleLauncher"
    classpath = sourceSets["test"].runtimeClasspath
    args = listOf("--select-class=org.http4k.stream.HelidonStreamingTest")
    jvmArgs = listOf("--enable-preview")
}
