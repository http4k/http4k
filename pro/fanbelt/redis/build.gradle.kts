description = "http4k Fanbelt on Redis: a backplane and shared presence"

plugins {
    id("org.http4k.pro")
}

dependencies {
    api(project(":http4k-fanbelt-core"))
    api(libs.lettuce.core)

    testImplementation(testFixtures(project(":http4k-fanbelt-core")))
    testImplementation(project(":http4k-fanbelt-testing"))
    testImplementation(libs.testcontainers)
}
