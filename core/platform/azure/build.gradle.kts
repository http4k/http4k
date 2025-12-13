description = "http4k Azure integration tooling"

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
    api(libs.azure.core)
    testImplementation(project(":http4k-testing-hamkrest"))
    testImplementation(libs.testcontainers.azure)
    testImplementation(libs.azure.storage.blob)

    testFixturesApi(testFixtures(project(":http4k-core")))
    testFixturesImplementation(project(":http4k-platform-core"))
    testFixturesImplementation(project(":http4k-config"))
    testFixturesImplementation(project(":http4k-client-okhttp"))
}

