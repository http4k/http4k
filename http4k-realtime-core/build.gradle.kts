description = "Http4k WebSocket core"

dependencies {
    api(project(":http4k-core"))
    testFixturesImplementation(testFixtures(project(":http4k-core")))
    testFixturesApi(project(":http4k-client-websocket"))
    testFixturesApi(project(":http4k-testing-hamkrest"))
}
