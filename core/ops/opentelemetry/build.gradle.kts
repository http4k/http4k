import org.http4k.internal.ModuleLicense.Apache2

description = "http4k OpenTelemetry support"

val license by project.extra { Apache2 }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
    api(project(":http4k-ops-core"))
    api(platform(libs.opentelemetry.bom))
    api("io.opentelemetry:opentelemetry-sdk")
    api("io.opentelemetry:opentelemetry-sdk-metrics")
    api(libs.opentelemetry.semconv)
    api(libs.opentelemetry.semconv.incubating)
    testFixturesApi("io.opentelemetry:opentelemetry-extension-trace-propagators")
    testFixturesApi("io.opentelemetry:opentelemetry-sdk-testing")

    testImplementation(testFixtures(project(":http4k-core")))
}

