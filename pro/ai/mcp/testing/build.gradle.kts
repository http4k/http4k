description = "http4k AI MCP Testing tools"

plugins {
    id("org.http4k.pro")
//    id("com.google.devtools.ksp")
}

dependencies {
    api(project(":http4k-ai-mcp-sdk"))
    api(project(":http4k-ai-mcp-client"))
    api(project(":http4k-server-helidon"))
    api(project(":http4k-template-handlebars"))

//    ksp(libs.kotshi.compiler)
}
