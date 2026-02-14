

description = "http4k AI MCP server SDK"

plugins {
    id("org.http4k.pro")
}

dependencies {
    api(project(":http4k-ai-mcp-core"))
    api(project(":http4k-ai-mcp-client"))
    api(project(":http4k-security-oauth"))
    implementation(project(":http4k-ops-opentelemetry"))

    testImplementation(project(":http4k-serverless-lambda"))
    testImplementation(project(":http4k-client-websocket"))

    testImplementation(libs.jsoup)
    testImplementation(libs.fs4k)

    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(project(":http4k-server-jetty"))
    testImplementation(project(":http4k-config"))
    testImplementation(project(":http4k-security-oauth"))
    testImplementation(project(":http4k-testing-approval"))
}
