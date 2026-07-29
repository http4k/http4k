
description = "http4k AI MCP Testing tools (Stateless)"

plugins {
    id("org.http4k.pro")
    id("com.google.devtools.ksp")
}

dependencies {
    api(project(":http4k-ai-mcp-stateless-sdk"))
    api(project(":http4k-ai-mcp-stateless-client"))
    api(project(":http4k-template-pebble"))

    ksp(libs.kotshi.compiler)

    testImplementation(project(":http4k-server-jetty"))
    testImplementation(project(":http4k-testing-approval"))
    testImplementation(project(":http4k-testing-webdriver"))
}
