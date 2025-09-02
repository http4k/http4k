import org.http4k.internal.ModuleLicense.Apache2

plugins {
    id("org.http4k.community")
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
