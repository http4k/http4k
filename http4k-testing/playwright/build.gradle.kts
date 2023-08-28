description = "http4k extensions for testing with Playwright"

dependencies {
    api(project(":http4k-core"))
    api("com.microsoft.playwright:playwright:_")
    implementation(Testing.junit.jupiter.api)

    testImplementation(project(path = ":http4k-core", configuration = "testArtifacts"))

}
