title: http4k Metrics Modules
description: Feature overview of the http4k-metrics modules

### Installation (Gradle)

```kotlin
dependencies {
    implementation(platform("org.http4k:http4k-bom:5.1.1.1"))
    implementation("org.http4k:http4k-metrics-micrometer")
}
```

### About

This module provides configurable Filters to provide metrics for http4k apps, plugging into the awesome [Micrometer](http://micrometer.io/) library.

### Micrometer [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/reference/micrometer/example.kt)

Both Server and Client filters are available for recording request counts and latency, optionally overriding values for the metric names, descriptions and request identification.

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/reference/micrometer/example.kt"></script>
