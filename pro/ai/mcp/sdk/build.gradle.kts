import org.http4k.internal.ModuleLicense.Http4kCommercial

description = "http4k AI MCP server SDK"

val license by project.extra { Http4kCommercial }

plugins {
    id("org.http4k.pro")
}

dependencies {
    api(project(":http4k-ai-mcp-core"))
    api(project(":http4k-ai-mcp-client"))
    api(project(":http4k-security-oauth"))

    testImplementation(project(":http4k-serverless-lambda"))
    testImplementation(project(":http4k-client-websocket"))

    testImplementation("org.jsoup:jsoup:_")
    testImplementation("dev.forkhandles:fs4k")

    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(project(":http4k-server-jetty"))
    testImplementation(project(":http4k-config"))
    testImplementation(project(":http4k-security-oauth"))
    testImplementation(project(":http4k-testing-approval"))
}
