

description = "http4k AI MCP Client support"

plugins {
    id("org.http4k.pro")
}

dependencies {
    api(project(":http4k-ai-mcp-core"))
    api(project(":http4k-security-oauth"))

    implementation(project(":http4k-ai-llm-core"))

    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(testFixtures(project(":http4k-ai-llm-core")))

    testImplementation(project(":http4k-ai-mcp-sdk"))
    testImplementation(project(":http4k-server-helidon"))
    testImplementation(project(":http4k-security-core"))
    testImplementation(project(":http4k-testing-approval"))
}
