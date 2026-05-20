plugins {
    id("org.http4k.conventions")
}

dependencies {
    testImplementation(project(":http4k-core"))
    testImplementation(project(":http4k-template-handlebars"))
    testImplementation(project(":http4k-testing-webdriver"))
    testImplementation(project(":http4k-client-apache"))
    testImplementation(libs.konsist)

    testImplementation(project(":http4k-ai-mcp-sdk"))
    testImplementation(project(":http4k-ai-mcp-testing"))
    testImplementation(project(":http4k-ops-opentelemetry"))
    testImplementation(project(":http4k-server-jetty"))
    testImplementation(project(":http4k-wiretap"))

    testImplementation(project(":http4k-testing-hamkrest"))

}
