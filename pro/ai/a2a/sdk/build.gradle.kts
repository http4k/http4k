description = "http4k AI A2A server SDK"

plugins {
    id("org.http4k.pro")
}

dependencies {
    api(project(":http4k-ai-a2a-core"))
    api(project(":http4k-realtime-core"))

    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(project(":http4k-server-jetty"))
    testImplementation(project(":http4k-testing-approval"))
}
