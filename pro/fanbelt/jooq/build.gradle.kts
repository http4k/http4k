description = "http4k Fanbelt storage over jOOQ"

plugins {
    id("org.http4k.pro")
}

dependencies {
    api(project(":http4k-fanbelt"))
    api(libs.jooq)

    testImplementation(project(":http4k-fanbelt-sqlite"))
    testImplementation(testFixtures(project(":http4k-fanbelt")))
    testImplementation(testFixtures(project(":http4k-fanbelt-exposed")))
    testRuntimeOnly(libs.postgresql)
}
