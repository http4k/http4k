
description = "http4k AI MCP Testing tools"

plugins {
    id("org.http4k.pro")
    id("com.google.devtools.ksp")
}

dependencies {
    api(project(":http4k-ai-mcp-sdk"))
    api(project(":http4k-ai-mcp-client"))

    ksp(libs.kotshi.compiler)

    testImplementation(project(":http4k-server-jetty"))
    testImplementation(project(":http4k-template-handlebars"))
}
