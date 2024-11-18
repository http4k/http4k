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

fun includeWithName(projectName: String, file: String = projectName, prefix: String = "connect") {
    val projectName = ":http4k-$projectName"
    include(projectName)
    project(projectName).projectDir = File("$prefix/$file")
}

fun String.includeModule(name: String) {
    val projectName = "http4k-$this-$name"
    include(":$projectName")
    project(":$projectName").projectDir = File("core/$this/${name.replace(':', '/')}")
}

includeWithName("bom", "bom", prefix = ".")

includeWithName("core", prefix = "core")
includeWithName("aws", prefix = "core")
includeWithName("azure", prefix = "core")

"client".apply {
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

includeWithName("cloudevents", prefix = "core")
includeWithName("cloudnative", prefix = "core")
includeWithName("config", prefix = "core")

"contract".apply {
    includeWithName(this, prefix = "core")
    includeModule("jsonschema")
    includeModule("ui-swagger")
    includeModule("ui-redoc")
}

"format".apply {
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

includeWithName("graphql", prefix = "core")
includeWithName("htmx", prefix = "core")
includeWithName("incubator", prefix = "core")
includeWithName("jsonrpc", prefix = "core")
includeWithName("metrics-micrometer", prefix = "core")
includeWithName("multipart", prefix = "core")
includeWithName("failsafe", prefix = "core")
includeWithName("resilience4j", prefix = "core")
includeWithName("opentelemetry", prefix = "core")
includeWithName("realtime-core", prefix = "core")

"server".apply {
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

"serverless".apply {
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

"security".apply {
    includeModule("core")
    includeModule("digest")
    includeModule("oauth")
}

"template".apply {
    includeModule("core")
    includeModule("freemarker")
    includeModule("handlebars")
    includeModule("jte")
    includeModule("rocker")
    includeModule("pebble")
    includeModule("thymeleaf")
    includeModule("pug4j")
}

"testing".apply {
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

includeWithName("webhook", prefix = "core")

includeWithName("tools")

//connect

includeWithName("connect-ksp-generator", "ksp-generator")

includeWithName("connect-bom", "bom")
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
    val projectName = "connect-$system"
    includeWithName(projectName, "$system/client")
    includeWithName("$projectName-fake", "$system/fake")
    extraModules.forEach {
        includeWithName("$projectName-$it", "$system/$it")
    }
}

fun includeVendorSystem(owner: String, system: String) {
    val projectName = "connect-$owner-$system"
    includeWithName(projectName, "$owner/$system/client")
    includeWithName("$projectName-fake", "$owner/$system/fake")
}

fun includeCommon(projectName: String, file: String) {
    includeWithName("connect-$projectName", file)
}

fun includeStorage(name: String) {
    includeWithName("connect-storage-$name", "storage/$name")
}
