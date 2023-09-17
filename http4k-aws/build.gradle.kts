description = "Http4k AWS integration and request signing"

dependencies {
    api(project(":http4k-core"))
    api(platform("software.amazon.awssdk:bom:_"))
    implementation("software.amazon.awssdk:http-client-spi")
    testImplementation(project(path = ":http4k-core", configuration = "testArtifacts"))
    testImplementation(project(":http4k-cloudnative"))
    testImplementation(project(":http4k-client-okhttp"))
}

