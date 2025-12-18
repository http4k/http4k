

description = "http4k Realtime core"

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))

    implementation(project(":http4k-security-core"))

    testFixturesApi(testFixtures(project(":http4k-core")))
    testImplementation(testFixtures(project(":http4k-core")))

    testImplementation(project(":http4k-server-helidon"))

    testFixturesApi(libs.mockk)
    testFixturesApi(project(":http4k-client-websocket"))
    testFixturesApi(project(":http4k-testing-hamkrest"))
    testFixturesApi(project(":http4k-web-datastar"))
    testFixturesApi(libs.okhttp.eventsource)
}
