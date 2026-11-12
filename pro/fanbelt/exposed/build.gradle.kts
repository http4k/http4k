description = "http4k Fanbelt storage and presence over Exposed"

plugins {
    id("org.http4k.pro")
}

dependencies {
    api(project(":http4k-fanbelt"))
    api(libs.exposed.core)
    api(libs.exposed.jdbc)
    implementation(libs.exposed.json)

    testFixturesApi(testFixtures(project(":http4k-fanbelt")))
    testFixturesApi(libs.testcontainers.postgresql)

    testRuntimeOnly(libs.postgresql)
}
