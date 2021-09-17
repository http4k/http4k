description = "Http4k OpenTelemetry support"

dependencies {
    api(project(":http4k-core"))
    api("io.opentelemetry:opentelemetry-sdk:_")
    api("io.opentelemetry:opentelemetry-sdk-metrics:_")
    testApi("io.opentelemetry:opentelemetry-extension-trace-propagators:_")
    testApi("io.opentelemetry:opentelemetry-extension-aws:_")
    testApi("io.opentelemetry:opentelemetry-exporters-inmemory:_")

    testImplementation(project(path = ":http4k-core", configuration ="testArtifacts"))
}
