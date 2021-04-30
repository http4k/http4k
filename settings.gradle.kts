rootProject.name = "http4k"

// TODO: remove pluginManagement bloc once a stable version of refreshVersions is released
pluginManagement {
    val version = "0.9.8-dev-004" // O.9.7 0.9.8:PR 0.9.8-SNAPSHOT
    repositories {
        gradlePluginPortal()
        when {
            version.contains(":PR") -> maven("https://jitpack.io")
            version.contains("-dev-") -> maven("https://dl.bintray.com/jmfayard/maven")
            version.contains("-SNAPSHOT") -> mavenLocal()
        }
    }
    resolutionStrategy {
        fun module(module: String) = when {
            version.contains(":PR") -> "com.github.jmfayard:refreshVersions:$version"
            else -> "de.fayard.refreshVersions:$module:$version"
        }
        eachPlugin {
            when (requested.id.id) {
                "de.fayard.refreshVersions" -> useModule(module("refreshVersions"))
                "de.fayard.buildSrcLibs" -> useModule(module("buildSrcLibs"))
            }
        }
    }
}

plugins {
    id("de.fayard.buildSrcLibs")
    id("de.fayard.refreshVersions")
}

fun String.includeModule(name: String) {
    val projectName = "$this-$name"
    include(":$projectName")
    project(":$projectName").projectDir = File("$this/${name.replace(':','/')}")
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
    includeModule("jetty")
    includeModule("okhttp")
    includeModule("websocket")
}
include("http4k-cloudevents")
include("http4k-cloudnative")
include("http4k-contract")
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
    includeModule("xml")
}
include("http4k-graphql")
include("http4k-incubator")
include("http4k-jsonrpc")
include("http4k-metrics-micrometer")
include("http4k-multipart")
include("http4k-resilience4j")
include("http4k-opentelemetry")
include("http4k-security-oauth")
include("http4k-realtime-core")

"http4k-server".apply {
    includeModule("apache")
    includeModule("apache4")
    includeModule("jetty")
    includeModule("ktorcio")
    includeModule("ktornetty")
    includeModule("netty")
    includeModule("ratpack")
    includeModule("undertow")
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
    includeModule("pebble")
    includeModule("thymeleaf")
    includeModule("jade4j")
}

"http4k-testing".apply {
    includeModule("approval")
    includeModule("chaos")
    includeModule("hamkrest")
    includeModule("kotest")
    includeModule("servirtium")
    includeModule("webdriver")
}
