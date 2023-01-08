description = "Http4k multipart form support"

dependencies {
    api(project(":http4k-core"))
    implementation(project(":http4k-format-core"))

    testImplementation(project(":http4k-format-moshi"))
    testImplementation(project(path = ":http4k-core", configuration = "testArtifacts"))
}
