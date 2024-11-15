@file:Suppress("UnstableApiUsage")

rootProject.name = "http4k"

pluginManagement {
    includeBuild("gradle/gradle-plugins")
}

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
include("http4k-azure")
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
include("http4k-config")

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
//    includeModule("azure:integration-test:test-function") // <-- removed due to bug with jackson in build phase
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

include("tools")

//connect
includeWithName("http4k-connect:tools", "http4k-connect:tools")

includeWithName("http4k-connect-ksp-generator", "http4k-connect-ksp-generator")

includeWithName("http4k-connect-bom", "bom")
includeSystem("core")
includeStorage("core")
includeStorage("jdbc")
includeStorage("redis")
includeStorage("s3")
includeStorage("http")

includeCommon("ai-core", "ai/core")
includeVendorSystem("ai", "anthropic")
includeVendorSystem("ai", "azure")
includeVendorSystem("ai", "lmstudio")
includeVendorSystem("ai", "openai")
includeVendorSystem("ai", "ollama")

includeCommon("ai-langchain", "ai/langchain")
includeCommon("ai-openai-plugin", "ai/openai/plugin")

includeCommon("amazon-core", "amazon/core")
includeVendorSystem("amazon", "apprunner")
includeVendorSystem("amazon", "cloudfront")
includeVendorSystem("amazon", "cloudwatchlogs")
includeVendorSystem("amazon", "cognito")
includeVendorSystem("amazon", "containercredentials")
includeVendorSystem("amazon", "eventbridge")
includeVendorSystem("amazon", "firehose")
includeVendorSystem("amazon", "instancemetadata")
includeVendorSystem("amazon", "dynamodb")
includeVendorSystem("amazon", "iamidentitycenter")
includeVendorSystem("amazon", "kms")
includeVendorSystem("amazon", "lambda")
includeVendorSystem("amazon", "s3")
includeVendorSystem("amazon", "secretsmanager")
includeVendorSystem("amazon", "sns")
includeVendorSystem("amazon", "ses")
includeVendorSystem("amazon", "sqs")
includeVendorSystem("amazon", "sts")
includeVendorSystem("amazon", "systemsmanager")
includeVendorSystem("amazon", "evidently")
includeSystem("example")

includeSystem("github")
includeSystem("gitlab")
includeSystem("mattermost")
includeSystem("openai", "plugin")

includeCommon("langchain", "langchain")

includeVendorSystem("kafka", "rest")
includeVendorSystem("kafka", "schemaregistry")

includeCommon("google-analytics-core", "google/analytics-core")
includeVendorSystem("google", "analytics-ua")
includeVendorSystem("google", "analytics-ga4")

fun includeSystem(system: String, vararg extraModules: String) {
    val projectName = "http4k-connect-$system"
    includeWithName(projectName, "$system/client")
    includeWithName("$projectName-fake", "$system/fake")
    extraModules.forEach {
        includeWithName("$projectName-$it", "$system/$it")
    }
}

fun includeVendorSystem(owner: String, system: String) {
    val projectName = "http4k-connect-$owner-$system"
    includeWithName(projectName, "$owner/$system/client")
    includeWithName("$projectName-fake", "$owner/$system/fake")
}

fun includeCommon(projectName: String, file: String) {
    includeWithName("http4k-connect-$projectName", file)
}

fun includeWithName(projectName: String, file: String) {
    include(":$projectName")
    project(":$projectName").projectDir = File("http4k-connect/$file")
}

fun includeStorage(name: String) {
    includeWithName("http4k-connect-storage-$name", "storage/$name")
}
