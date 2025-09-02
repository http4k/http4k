import org.http4k.internal.ModuleLicense.Apache2

description = "http4k Bridge: from Ktor to http4k"

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
    api(libs.ktor.server.core)

    api(libs.ktor.server.netty)
    testFixturesApi(testFixtures(project(":http4k-core")))
}
