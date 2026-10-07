

description = "http4k GCP integration tooling"

plugins {
    id("org.http4k.default-license")
}

dependencies {
    api(project(":http4k-core"))
    api(libs.google.http.client)

    testImplementation(project(":http4k-testing-hamkrest"))
    testFixturesImplementation(testFixtures(project(":http4k-core")))
    testFixturesImplementation(project(":http4k-platform-core"))
    testFixturesImplementation(project(":http4k-config"))
    testFixturesImplementation(project(":http4k-client-okhttp"))
}

