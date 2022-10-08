description = "Http4k OpenTelemetry support"

dependencies {
    api(project(":http4k-core"))
    api(platform("io.opentelemetry:opentelemetry-bom:_"))
    api("io.opentelemetry:opentelemetry-sdk")
    api("io.opentelemetry:opentelemetry-sdk-metrics")
    testApi("io.opentelemetry:opentelemetry-extension-trace-propagators")
    testApi("io.opentelemetry:opentelemetry-extension-aws")
    testApi("io.opentelemetry:opentelemetry-sdk-metrics-testing:_")

    testImplementation(project(path = ":http4k-core", configuration ="testArtifacts"))
}
