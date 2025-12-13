description = "http4k Azure integration tooling"

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
    api(libs.azure.core)
    testImplementation(project(":http4k-testing-hamkrest"))
    testImplementation("org.testcontainers:azure:1.21.3")
    testImplementation("com.azure:azure-storage-blob:12.32.0")

    testFixturesImplementation(testFixtures(project(":http4k-core")))
    testFixturesImplementation(project(":http4k-platform-core"))
    testFixturesImplementation(project(":http4k-config"))
    testFixturesImplementation(project(":http4k-client-okhttp"))
}

