description = "Http4k multipart form support"

dependencies {
    api(project(":http4k-core"))

    testImplementation(project(path = ":http4k-core", configuration = "testArtifacts"))
}
