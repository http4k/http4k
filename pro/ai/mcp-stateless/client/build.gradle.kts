

description = "http4k AI MCP Client support (Stateless)"

plugins {
    id("org.http4k.pro")
}

dependencies {
    api(project(":http4k-ai-mcp-stateless-core"))
    api(project(":http4k-security-oauth"))

    implementation(project(":http4k-ai-llm-core"))

    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(testFixtures(project(":http4k-ai-llm-core")))

    testImplementation(project(":http4k-ai-mcp-stateless-sdk"))
    testImplementation(project(":http4k-ai-mcp-stateless-testing"))
    testImplementation(project(":http4k-server-helidon"))
    testImplementation(project(":http4k-security-core"))
    testImplementation(project(":http4k-testing-approval"))
}
