title: http4k Resilience4J Module
description: Feature overview of the http4k-resilience4j module

### Installation (Gradle)

```groovy
implementation group: "org.http4k", name: "http4k-resilience4j", version: "4.34.1.0"
```

### About

This module provides configurable Filters to provide CircuitBreaking, RateLimiting, Retrying and Bulkheading, by integrating with the awesome [Resilience4J](http://resilience4j.github.io/resilience4j/) library.

### Circuit Breaking [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/reference/resilience4j/example_circuit.kt)
A Circuit Filter detects failures and then Opens for a set period to allow the underlying system to recover.

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/reference/resilience4j/example_circuit.kt"></script>

### Rate Limiting [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/reference/resilience4j/example_ratelimiter.kt)
A RateLimit Filter monitors the number of requests over a set window.

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/reference/resilience4j/example_ratelimiter.kt"></script>

### Retrying [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/reference/resilience4j/example_retrying.kt)
A Retrying Filter retries requests if a failure is generated.

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/reference/resilience4j/example_retrying.kt"></script>


### Bulkheading [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/reference/resilience4j/example_bulkheading.kt)
A Bulkhead Filter limits the amount of parallel calls that can be executed.

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/reference/resilience4j/example_bulkheading.kt"></script>
