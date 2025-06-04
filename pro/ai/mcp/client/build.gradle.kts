import org.http4k.internal.ModuleLicense.Http4kCommercial

description = "http4k AI MCP Client support"

val license by project.extra { Http4kCommercial }

plugins {
    id("org.http4k.pro")
}

dependencies {
    api(project(":http4k-ai-mcp-core"))
    api(project(":http4k-security-oauth"))
    
    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(project(":http4k-ai-mcp-sdk"))
    testImplementation(project(":http4k-server-jetty"))
    testImplementation(project(":http4k-security-core"))
    testImplementation(project(":http4k-testing-approval"))
}
