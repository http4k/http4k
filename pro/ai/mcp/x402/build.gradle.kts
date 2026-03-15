description = "http4k AI MCP x402 Payment Protocol integration"

plugins {
    id("org.http4k.pro")
}

dependencies {
    api(project(":http4k-ai-mcp-sdk"))
    api(project(":http4k-connect-x402"))

    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(project(":http4k-connect-x402-fake"))
    testImplementation(project(":http4k-server-jetty"))
    testImplementation(project(":http4k-ai-mcp-testing"))
}
