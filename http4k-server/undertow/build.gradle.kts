description = "Http4k HTTP Server built on top of Undertow"

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-realtime-core"))
    api("io.undertow:undertow-core:_")
    testImplementation(project(path = ":http4k-core", configuration = "testArtifacts"))
    testImplementation(project(path = ":http4k-realtime-core", configuration = "testArtifacts"))
}
