description = "http4k Apache Fory serialization support"

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-format-core"))
    api(project(":http4k-realtime-core"))
    api(libs.fory.core)
    api(libs.fory.kotlin)

    testImplementation(libs.values4k)
    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(testFixtures(project(":http4k-format-core")))
}
