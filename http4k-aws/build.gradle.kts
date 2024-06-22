description = "Http4k AWS integration and request signing"

dependencies {
    api(project(":http4k-core"))
    api(platform("software.amazon.awssdk:bom:_"))
    implementation("software.amazon.awssdk:http-client-spi")
    testImplementation(project(":http4k-client-okhttp"))
    testImplementation(project(":http4k-client-apache"))
    testImplementation(project(":http4k-testing-hamkrest"))

    testFixturesImplementation(testFixtures(project(":http4k-core")))
    testFixturesImplementation(project(":http4k-cloudnative"))
    testFixturesImplementation(project(":http4k-client-okhttp"))
}

