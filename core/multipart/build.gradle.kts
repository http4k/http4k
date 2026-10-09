

description = "http4k multipart form support"

plugins {
    id("org.http4k.default-license")
}

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-format-core"))

    testImplementation(project(":http4k-format-moshi"))
    testImplementation(testFixtures(project(":http4k-core")))
}
