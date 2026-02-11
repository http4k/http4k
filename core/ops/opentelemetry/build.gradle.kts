

description = "http4k OpenTelemetry support"

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-ops-core"))
    api(platform(libs.opentelemetry.bom))
    api(libs.opentelemetry.sdk)
    api(libs.opentelemetry.sdk.metrics)
    api(libs.opentelemetry.semconv)
    api(libs.opentelemetry.semconv.incubating)
    implementation(project(":http4k-realtime-core"))

    testFixturesApi(libs.opentelemetry.extension.trace.propagators)
    testFixturesApi(libs.opentelemetry.sdk.testing)
    testImplementation(testFixtures(project(":http4k-core")))
}

