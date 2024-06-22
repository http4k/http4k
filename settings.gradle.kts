@file:Suppress("UnstableApiUsage")

rootProject.name = "http4k"

plugins {
    id("de.fayard.refreshVersions").version("0.60.5")
}

refreshVersions {
    rejectVersionIf {
        candidate.stabilityLevel.isLessStableThan(current.stabilityLevel) ||
            setOf("milestone", "-RC").map { it.lowercase() }.any { candidate.value.contains(it) } ||
            Regex("""\d{4}-\d{2}-\d{2}T\d{2}-\d{2}-\d{2}.*""").matches(candidate.value) || // graphql nightlies
            candidate.value.contains("nf-execution") // graphql nightlies
    }
}

gradle.startParameter.isContinueOnFailure = true

fun String.includeModule(name: String) {
    val projectName = "$this-$name"
    include(":$projectName")
    project(":$projectName").projectDir = File("$this/${name.replace(':', '/')}")
}

fun String.includeSubmodule(name: String) {
    include(":$this-$name")
    project(":$this-$name").projectDir = File("$this/${name.replace('-', '/')}")
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
    includeModule("helidon")
    includeModule("jetty")
    includeModule("okhttp")
    includeModule("websocket")
}

include("http4k-cloudevents")
include("http4k-cloudnative")

"http4k-contract".apply {
    include(":$this")
    project(":$this").projectDir = File("$this/openapi")
    includeSubmodule("jsonschema")
    includeSubmodule("ui-swagger")
    includeSubmodule("ui-redoc")
}

"http4k-format".apply {
    includeModule("core")
    includeModule("argo")
    includeModule("dataframe")
    includeModule("gson")
    includeModule("jackson")
    includeModule("jackson-xml")
    includeModule("jackson-yaml")
    includeModule("klaxon")
    includeModule("kondor-json")
    includeModule("kotlinx-serialization")
    includeModule("moshi")
    includeModule("moshi-yaml")
    includeModule("xml")
    includeModule("jackson-csv")
}

include("http4k-graphql")
include("http4k-htmx")
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
    includeModule("helidon")
    includeModule("jetty")
    includeModule("jetty11")
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

"http4k-security".apply {
    includeModule("core")
    includeModule("digest")
    includeModule("oauth")
}

"http4k-template".apply {
    includeModule("core")
    includeModule("freemarker")
    includeModule("handlebars")
    includeModule("jte")
    includeModule("rocker")
    includeModule("pebble")
    includeModule("thymeleaf")
    includeModule("pug4j")
}

"http4k-testing".apply {
    includeModule("approval")
    includeModule("chaos")
    includeModule("hamkrest")
    includeModule("kotest")
    includeModule("playwright")
    includeModule("strikt")
    includeModule("servirtium")
    includeModule("tracerbullet")
    includeModule("webdriver")
}

include("http4k-webhook")
