description = "Http4k Azure integration tooling"

plugins {
    id("org.http4k.license-check")
    id("org.http4k.publishing")
    id("org.http4k.api-docs")
}

dependencies {
    api(project(":http4k-core"))
    api("com.azure:azure-core:_")
    testImplementation(project(":http4k-testing-hamkrest"))

    testFixturesImplementation(testFixtures(project(":http4k-core")))
    testFixturesImplementation(project(":http4k-cloudnative"))
    testFixturesImplementation(project(":http4k-client-okhttp"))
}

