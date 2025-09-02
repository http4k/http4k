

description = "http4k Bridge: from Jakarta to http4k"

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
    implementation(libs.jakarta.inject.api)
    implementation(libs.jakarta.ws.rs.api)

    testFixturesApi(testFixtures(project(":http4k-core")))
    testApi(libs.resteasy.core)
}
