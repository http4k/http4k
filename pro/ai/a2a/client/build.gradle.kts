description = "http4k AI A2A Client support"

plugins {
    id("org.http4k.pro")
}

dependencies {
    api(project(":http4k-ai-a2a-core"))
    api(project(":http4k-realtime-core"))

    testFixturesApi(project(":http4k-ai-a2a-sdk"))
    testFixturesApi(project(":http4k-testing-hamkrest"))
    testFixturesApi(project(":http4k-testing-approval"))

    testFixturesApi(project(":http4k-server-helidon"))
    testImplementation(testFixtures(project(":http4k-core")))
}
