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
    api(libs.opentelemetry.sdk)
    api(libs.opentelemetry.sdk.metrics)
    api(libs.opentelemetry.semconv)
    api(libs.opentelemetry.semconv.incubating)
    testFixturesApi(libs.opentelemetry.extension.trace.propagators)
    testFixturesApi(libs.opentelemetry.sdk.testing)

    testImplementation(testFixtures(project(":http4k-core")))
}

