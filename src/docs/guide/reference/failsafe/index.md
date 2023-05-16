title: http4k Failsafe Module
description: Feature overview of the http4k-failsafe module

### Installation (Gradle)

```kotlin
dependencies {
    implementation(platform("org.http4k:http4k-bom:4.44.0.0"))
    implementation("org.http4k:http4k-failsafe")
}
```

### About

This module provides a configurable Filter to provide fault tolerance (CircuitBreaking, RateLimiting, Retrying, Bulkheading, Timeouts etc.),
by integrating with the [Failsafe](https://failsafe.dev/) library.

### Basic example [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/reference/failsafe/example_bulkheading.kt)

Here's an example that uses BulkHeading to demonstrate how easy it is to use the filter with configured Failsafe policies.

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/reference/failsafe/example_bulkheading.kt"></script>

### Example of using multiple policies [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/reference/failsafe/example_multiple_policies.kt)

Using multiple Failsafe policies in the filter is just as easy, as the following example shows.

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/reference/failsafe/example_multiple_policies.kt"></script>

