description = "http4k Fanbelt storage and presence over Exposed"

plugins {
    id("org.http4k.default-license")
}

dependencies {
    api(project(":http4k-fanbelt"))
    api(libs.exposed.core)
    api(libs.exposed.jdbc)

    testFixturesApi(testFixtures(project(":http4k-fanbelt")))
}
