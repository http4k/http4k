description = "Http4k Azure integration tooling"

dependencies {
    api(project(":http4k-core"))
    api("com.azure:azure-core:_")
    testImplementation(project(":http4k-testing-hamkrest"))

    testFixturesImplementation(testFixtures(project(":http4k-core")))
    testFixturesImplementation(project(":http4k-cloudnative"))
    testFixturesImplementation(project(":http4k-client-okhttp"))
}

