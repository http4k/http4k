rootProject.name = "http4k"

plugins {
    id("de.fayard.refreshVersions").version("0.40.2")
}

refreshVersions {
    enableBuildSrcLibs()

    rejectVersionIf {
        candidate.stabilityLevel.isLessStableThan(current.stabilityLevel)
    }
}

fun String.includeModule(name: String) {
    val projectName = "$this-$name"
    include(":$projectName")
    project(":$projectName").projectDir = File("$this/${name.replace(':', '/')}")
}

fun includeWithDirectory(projectName: String, name: String) {
    include("$projectName-$name")
}

include("http4k-core")
include("http4k-aws")
include("http4k-bom")

"http4k-client".apply {
    includeModule("apache")
    includeModule("apache4")
    includeModule("apache-async")
    includeModule("apache4-async")
    includeModule("fuel")
    includeModule("jetty")
    includeModule("okhttp")
    includeModule("websocket")
}

include("http4k-cloudevents")
include("http4k-cloudnative")

include("http4k-contract")
"http4k-contract-ui".apply {
    includeModule("swagger")
    includeModule("redoc")
}

"http4k-format".apply {
    includeModule("core")
    includeModule("argo")
    includeModule("gson")
    includeModule("jackson")
    includeModule("jackson-xml")
    includeModule("jackson-yaml")
    includeModule("klaxon")
    includeModule("kotlinx-serialization")
    includeModule("moshi")
    includeModule("moshi-yaml")
    includeModule("xml")
    includeModule("jackson-csv")
}

include("http4k-graphql")
include("http4k-incubator")
include("http4k-jsonrpc")
include("http4k-metrics-micrometer")
include("http4k-multipart")
include("http4k-failsafe")
include("http4k-resilience4j")
include("http4k-opentelemetry")
include("http4k-realtime-core")

"http4k-server".apply {
    includeModule("apache")
    includeModule("apache4")
    includeModule("jetty")
    includeModule("ktorcio")
    includeModule("ktornetty")
    includeModule("netty")
    includeModule("ratpack")
    includeModule("shutdown-integration-test")
    includeModule("undertow")
    includeModule("websocket")
}

"http4k-serverless".apply {
    includeModule("core")
    includeModule("alibaba")
    includeModule("alibaba:integration-test:test-function")
    includeModule("azure")
    includeModule("azure:integration-test:test-function")
    includeModule("gcf")
    includeModule("gcf:integration-test:test-function")
    includeModule("lambda")
    includeModule("lambda-runtime")
    includeModule("lambda:integration-test")
    includeModule("lambda:integration-test:test-function")
    includeModule("openwhisk")
    includeModule("openwhisk:integration-test")
    includeModule("openwhisk:integration-test:test-function")
    includeModule("tencent")
    includeModule("tencent:integration-test:test-function")
}

"http4k-template".apply {
    includeModule("core")
    includeModule("dust")
    includeModule("freemarker")
    includeModule("handlebars")
    includeModule("rocker")
    includeModule("pebble")
    includeModule("thymeleaf")
    includeModule("jade4j")
}

"http4k-security".apply {
    includeModule("core")
    includeModule("digest")
    includeModule("oauth")
}

"http4k-testing".apply {
    includeModule("approval")
    includeModule("chaos")
    includeModule("hamkrest")
    includeModule("kotest")
    includeModule("strikt")
    includeModule("servirtium")
    includeModule("webdriver")
}
