title: http4k How-to: Make HTTP calls in parallel
description: Recipe to make HTTP calls in parallel using a ThreadPoolExecutor

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/howto/moshi_lite/example.kt"></script>

There are cases where an application needs to make multiple HTTP calls to other services as part of handling a particular request. 
As a general rule-of-thumb, we recommend people to avoid [premature optimisation], however sometimes the quantity of calls or performance of other services demand those to be executed in parallel.

In this example, we show how to use a [ThreadPoolExecutor] to manage multiple HTTP calls in parallel, and synchronise their results to produce a single response.

This recipe also covers how to make [distributed tracing] work when tracing information is consumed by multiple threads.

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/howto/monitor_http4k/example_debugging.kt"></script>

[premature optimisation]: https://wiki.c2.com/?PrematureOptimization
[distributed tracing]: /guide/howto/monitor_http4k/#distributed_tracing
[ThreadPoolExecutor]: https://www.baeldung.com/thread-pool-java-and-guava
