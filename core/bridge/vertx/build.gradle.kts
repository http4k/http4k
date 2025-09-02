

description = "http4k Bridge: from Vertx to http4k"

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
    implementation(libs.vertx.web)

    testFixturesApi(testFixtures(project(":http4k-core")))
}
