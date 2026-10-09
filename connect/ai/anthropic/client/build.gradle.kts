

plugins {
    id("org.http4k.default-license")
    id("org.http4k.connect.module")
    id("org.http4k.connect.client")
}

dependencies {
    api(project(":http4k-ai-core"))
    api(project(":http4k-multipart"))

    api(libs.kotshi.api)

    testImplementation(project(":http4k-format-moshi"))
    testFixturesApi(testFixtures(project(":http4k-connect-core")))
}
