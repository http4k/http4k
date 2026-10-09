

plugins {
    id("org.http4k.default-license")
    id("org.http4k.connect.module")
    id("org.http4k.connect.storage")
}

dependencies {
    api(project(":http4k-format-moshi"))
    api(libs.exposed.core)
    api(libs.exposed.jdbc)

    testFixturesApi(libs.hikaricp)
    testFixturesApi(libs.h2)
}
