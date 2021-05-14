title: http4k OpenTelemetry Modules
description: Feature overview of the http4k-opentelemetry module

### Installation (Gradle)

```groovy
implementation group: "org.http4k", name: "http4k-opentelemetry", version: "4.9.0.1"
```

### About

This module provides configurable Filters to provide distributed tracing and metrics for http4k apps, plugging into the awesome [OpenTelemetry](https://opentelemetry.io/) APIs.

`OpenTelemetry is a collection of tools, APIs, and SDKs. You use it to instrument, generate, collect, and export telemetry data (metrics, logs, and traces) for analysis in order to understand your software's performance and behavior.`

### Tracing [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/modules/opentelemetry/example_tracing.kt)

OpenTelemetry provides a pluggable interface for tracing propagation, so you can easily switch between different implementations such as AWS X-Ray, B3 and Jaeger etc.

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/modules/opentelemetry/example_tracing.kt"></script>

### Metrics [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/modules/opentelemetry/example_metrics.kt)

Both Server and Client filters are available for recording request counts and latency, optionally overriding values for the metric names, descriptions and request identification.

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/modules/opentelemetry/example_metrics.kt"></script>
