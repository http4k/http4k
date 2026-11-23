description = "http4k Fanbelt storage over jOOQ"

plugins {
    id("org.http4k.default-license")
}

dependencies {
    api(project(":http4k-fanbelt"))
    api(libs.jooq)

    testImplementation(testFixtures(project(":http4k-fanbelt")))
    testImplementation(testFixtures(project(":http4k-fanbelt-exposed")))
    testImplementation(project(":http4k-fanbelt-postgres"))
    testImplementation(testFixtures(project(":http4k-fanbelt-postgres")))
}
