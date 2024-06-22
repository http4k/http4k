description = "Http4k OpenTelemetry support"

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
