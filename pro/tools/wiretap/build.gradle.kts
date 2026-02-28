description = "http4k Wiretap"

plugins {
    id("org.http4k.conventions")
//    id("org.http4k.pro")
}

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-api-openapi"))
    api(project(":http4k-format-moshi"))
    api(project(":http4k-ai-mcp-sdk"))
    api(project(":http4k-ops-opentelemetry"))
    api(project(":http4k-realtime-core"))
    api(project(":http4k-template-handlebars"))
    api(project(":http4k-testing-chaos"))
    api(project(":http4k-web-datastar"))
    testImplementation(project(":http4k-testing-approval"))
    testImplementation(project(":http4k-testing-hamkrest"))
    testImplementation(project(":http4k-server-jetty"))
}
