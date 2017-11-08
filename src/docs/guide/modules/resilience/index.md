title: http4k Resilience4J Module
description: Feature overview of the http4k-resilience4j module

### Installation (Gradle)
```compile group: "org.http4k", name: "http4k-resilience4j", version: "2.37.0"```

### About

This module provides configurable Filters to provide CircuitBreaking, RateLimiting, Retrying and Bulkheading, by integrating with the awesome [Resilience4J](http://resilience4j.github.io/resilience4j/) library.

### Circuit Breaking
A Circuit Filter detects failures and then Opens for a set period to allow the underlying system to recover.

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/modules/resilience/example_circuit.kt"></script>

### Rate Limiting
A RateLimit Filter monitors the number of requests over a set window.

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/modules/resilience/example_ratelimiter.kt"></script>

### Retrying
A Retrying Filter retries requests if a failure is generated.

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/modules/resilience/example_retrying.kt"></script>


### Bulkheading
A Bulkhead Filter limits the amount of parallel calls that can be executed.

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/modules/resilience/example_bulkheading.kt"></script>
