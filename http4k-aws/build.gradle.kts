description = "Http4k AWS integration and request signing"

dependencies {
    api(project(":http4k-core"))
    implementation("software.amazon.awssdk:http-client-spi:_")
    testImplementation(project(path = ":http4k-core", configuration = "testArtifacts"))
    testImplementation(project(":http4k-cloudnative"))
}

