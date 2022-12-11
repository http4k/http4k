description = "Http4k HTTP Server built on top of Helidon Nima"

dependencies {
    api(project(":http4k-core"))
    api("io.helidon.nima.webserver:helidon-nima-webserver:_")
    testImplementation(project(path = ":http4k-core", configuration = "testArtifacts"))
}
