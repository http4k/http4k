description = "http4k MCP-to-A2A bridge"

plugins {
    id("org.http4k.pro")
}

dependencies {
    api(project(":http4k-ai-a2a-client"))
    api(project(":http4k-ai-mcp-sdk"))
    api(libs.kotlin.reflect)

    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(project(":http4k-ai-a2a-sdk"))
}
