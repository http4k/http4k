description = "http4k AI AG-UI server SDK"

plugins {
    id("org.http4k.pro")
}

dependencies {
    api(project(":http4k-ai-ag-ui-core"))

    testImplementation(testFixtures(project(":http4k-core")))
    testImplementation(project(":http4k-ai-ag-ui-client"))
    testImplementation(project(":http4k-testing-approval"))

    // examples
    testImplementation(project(":http4k-ai-llm-openai"))
    testImplementation(project(":http4k-ai-mcp-sdk"))
    testImplementation(project(":http4k-ai-mcp-client"))
    testImplementation(project(":http4k-server-jetty"))
}
