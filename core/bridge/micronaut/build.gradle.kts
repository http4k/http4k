import org.http4k.internal.ModuleLicense.Apache2

description = "http4k Bridge: from Micronaut to http4k"

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))

    implementation(libs.micronaut.http)

    testFixturesApi(testFixtures(project(":http4k-core")))
}
