description = "Http4k Failsafe support"

dependencies {
    api(project(":http4k-core"))
    api("dev.failsafe:failsafe:_")
    testImplementation(project(path = ":http4k-core", configuration = "testArtifacts"))
}
