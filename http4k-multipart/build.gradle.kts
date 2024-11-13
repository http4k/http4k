description = "Http4k multipart form support"

plugins {
    id("org.http4k.apache-module")
}

dependencies {
    api(project(":http4k-core"))
    implementation(project(":http4k-format-core"))

    testImplementation(project(":http4k-format-moshi"))
    testImplementation(testFixtures(project(":http4k-core")))
}
