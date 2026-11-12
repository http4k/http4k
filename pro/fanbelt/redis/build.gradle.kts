description = "http4k Fanbelt on Redis: a backplane and shared presence"

plugins {
    id("org.http4k.pro")
}

dependencies {
    api(project(":http4k-fanbelt"))
    api(libs.lettuce.core)

    testImplementation(testFixtures(project(":http4k-fanbelt")))
    testImplementation(project(":http4k-fanbelt-testing"))
    testImplementation(libs.testcontainers)
}
