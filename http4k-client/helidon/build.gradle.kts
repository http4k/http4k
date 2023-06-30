description = "HTTP Client built on top of Helidon"

dependencies {
    api(project(":http4k-core"))
    api("io.helidon.nima.webclient:helidon-nima-webclient:_")
    testImplementation(project(path = ":http4k-core", configuration = "testArtifacts"))
}
