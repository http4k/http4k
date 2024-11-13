description = "Http4k Moshi JSON support"

plugins {
    id("org.http4k.module")
}

dependencies {
    api(project(":http4k-format-core"))
    api(project(":http4k-realtime-core"))
    api(Square.moshi)
    api(Square.moshi.kotlinReflect)
    implementation("dev.forkhandles:values4k:_")

    testImplementation(project(":http4k-core"))
    testImplementation(Square.moshi)

    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(testFixtures(project(":http4k-format-core")))
}
