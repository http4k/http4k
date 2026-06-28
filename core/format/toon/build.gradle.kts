

description = "http4k Toon support using the official Toon Java SDK"

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-realtime-core"))
    api(project(":http4k-format-core"))
    api(project(":http4k-format-moshi"))
    api(libs.jtoon)

    testImplementation(project(":http4k-core"))
    testImplementation(libs.values4k)
    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(testFixtures(project(":http4k-format-core")))
}
