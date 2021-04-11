title: http4k SSE APIs
description: Recipes for using http4k with Server-Sent Events

### Gradle setup

```groovy
implementation group: "org.http4k", name: "http4k-core", version: "4.7.0.1"
implementation group: "org.http4k", name: "http4k-server-undertow", version: "4.7.0.1"
```

**http4k** provides SSE (Server Sent Events) support using a simple, consistent, typesafe, and testable API on supported server backends (see above). SSE communication consists of 3 main concepts:

1. `SseHandler` - represented as a typealias: `SseHandler =  (Request) -> SseConsumer?`. This is responsible for matching an HTTP request to an SSE handler.
1. `SseConsumer` - represented as a typealias: `SseConsumer = (Sse) -> Unit`. This function is called on connection of a Sse and allow the API user to receive to events coming from the connected SSE handler.
1. `SseMessage` - a message which is sent from the SSE handler SseMessages are immutable data classes.

### SSE as a Function
The simplest possible SSE handler can be mounted as a `SseConsumer` function onto a server with:
```kotlin
{ sse: Sse -> sse.send(SseMessage.Data("hello")) }.asServer(Undertow(9000)).start()
```

### Mixing HTTP and SSE services [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/cookbook/sse/example_polyhandler.kt)
Both SSE and Http handlers in **http4k** are routed using a similar path-based API. We combine them into a single `PolyHandler`. SSE handlers react to HTTP traffic which send an `Accept` header with `text/event-stream` value:

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/cookbook/sse/example_polyhandler.kt"></script>
