title: http4k How-to: Monitor http4k
description: How to monitor http4k endpoints and applications

Measuring performance of application estate is crucial in today's microservice world - it is crucial that dev-ops enabled teams can monitor, react and scale dynamically to changes in the runtime environment. However, because of the plethora of monitoring tools on the market, and because [**http4k**](https://github.com/http4k/http4k) is a toolkit and not a complete "batteries included" framework, it provides a number of integration points to enable monitoring systems to be plugged in as required. Additionally, it is envisaged that users will probably want to provide their own implementations of the [**http4k**](https://github.com/http4k/http4k) `ServerConfig` classes (`Jetty`, `Undertow` etc..) so that tweaking and tuning to their exact requirements is accessible, instead of [**http4k**](https://github.com/http4k/http4k) attempting to provide some generic configuration API to achieve it.

### Gradle setup

```groovy
implementation group: "org.http4k", name: "http4k-core", version: "4.25.10.1"
implementation group: "org.http4k", name: "http4k-metrics-micrometer", version: "4.25.10.1"
```
 
### Metrics (Micrometer) [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/howto/monitor_http4k/example_micrometer.kt)

[**http4k**](https://github.com/http4k/http4k) provides module support for monitoring application endpoints using the [**micrometer**](http://micrometer.io/) metrics abstraction library, which currently enables support for libraries such as Graphite, StatsD, Prometheus and Netflix Atlas. This also provides drop-in classes to record stats such as JVM performance, GC and thread usage.

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/howto/monitor_http4k/example_micrometer.kt"></script>

### Metrics (other APIs) [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/howto/monitor_http4k/example_metrics.kt)

Alternatively, it's very easy to use a standard `Filter` to report on stats:

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/howto/monitor_http4k/example_metrics.kt"></script>

### Logging [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/howto/monitor_http4k/example_logging.kt)
This is trivial to achieve by using a Filter:
 
<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/howto/monitor_http4k/example_logging.kt"></script>

### Distributed tracing [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/howto/monitor_http4k/example_tracing.kt)
This allows a chain of application calls to be tied together and is generally done through the setting of HTTP headers on each call. [**http4k**](https://github.com/http4k/http4k) supports the [OpenZipkin](https://zipkin.io/) standard for achieving this and provides both Server-side and Client-side `Filters` for this purpose. This example shows a chain of two proxies and an endpoint - run it to observe the changes to the tracing headers as the request flows through the system:

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/howto/monitor_http4k/example_tracing.kt"></script>

### Debugging [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/howto/monitor_http4k/example_debugging.kt)
Easily wrap an `HttpHandler` in a debugging filter to check out what is going on under the covers:
 
<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/howto/monitor_http4k/example_debugging.kt"></script>
