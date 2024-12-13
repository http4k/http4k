import org.http4k.internal.ModuleLicense.Http4kCommunity

description = "DEPRECATED: Use http4k-ops-opentelemetry"

val license by project.extra { Http4kCommunity }

plugins {
    id("org.http4k.community")
}

dependencies {
    api(project(":http4k-core"))
    api(platform("io.opentelemetry:opentelemetry-bom:_"))
    api("io.opentelemetry:opentelemetry-sdk")
    api("io.opentelemetry:opentelemetry-sdk-metrics")
    api("io.opentelemetry.semconv:opentelemetry-semconv:_")
    api("io.opentelemetry.semconv:opentelemetry-semconv-incubating:_")
    testFixturesApi("io.opentelemetry:opentelemetry-extension-trace-propagators")
    testFixturesApi("io.opentelemetry:opentelemetry-sdk-testing")

    testImplementation(testFixtures(project(":http4k-core")))
}
