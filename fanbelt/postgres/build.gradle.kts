description = "http4k Fanbelt document storage and backplane transport on Postgres"

plugins {
    id("org.http4k.default-license")
}

dependencies {
    api(project(":http4k-fanbelt-exposed"))
    api(libs.postgresql)
    implementation(libs.exposed.json)

    testFixturesApi(libs.testcontainers.postgresql)

    testImplementation(testFixtures(project(":http4k-fanbelt-exposed")))
    testImplementation(testFixtures(project(":http4k-fanbelt")))
}
