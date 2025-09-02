

description = "http4k Bridge: Servlet to http4k"

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))

    implementation(libs.javax.servlet.api)
    implementation(libs.jakarta.servlet.api)

    testFixturesImplementation(libs.javax.servlet.api)
    testFixturesImplementation(libs.jakarta.servlet.api)
    testFixturesApi(libs.mock4k)
}
