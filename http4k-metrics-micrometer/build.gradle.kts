description = "Http4k metrics support, integrating with micrometer.io"

dependencies {
    api(project(":http4k-core"))
    api("io.micrometer:micrometer-core:_")
    testImplementation(project(path = ":http4k-core", configuration = "testArtifacts"))
}
