title: http4k Failsafe Module
description: Feature overview of the http4k-failsafe module

### Installation (Gradle)

```groovy
implementation group: "org.http4k", name: "http4k-failsafe", version: "4.34.4.0"
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

