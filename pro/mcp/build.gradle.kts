plugins {
    id("org.http4k.conventions")
}

dependencies {
    testImplementation(project(":http4k-mcp-sdk"))
    testImplementation(project(":http4k-mcp-desktop"))
    testImplementation(project(":http4k-server-helidon"))
    testImplementation(testFixtures(project(":http4k-core")))
}
