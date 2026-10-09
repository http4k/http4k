

plugins {
    id("org.http4k.default-license")
    id("org.http4k.connect.module")
    id("org.http4k.connect.fake")
}

dependencies {
    testFixturesApi(libs.kotshi.api)

    testFixturesApi(project(":http4k-connect-kafka-rest"))
}
