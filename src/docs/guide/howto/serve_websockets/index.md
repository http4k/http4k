title: http4k How-to: Serve Websocket
description: Recipes for using http4k with websockets

### Gradle setup

```groovy
implementation group: "org.http4k", name: "http4k-core", version: "4.32.0.0"
implementation group: "org.http4k", name: "http4k-server-jetty", version: "4.32.0.0"
implementation group: "org.http4k", name: "http4k-client-websocket", version: "4.32.0.0"
implementation group: "org.http4k", name: "http4k-format-jackson", version: "4.32.0.0"
```

**http4k** provides Websocket support using a simple, consistent, typesafe, and testable API on supported server backends (see above). Websocket communication consists of 4 main concepts:

1. `WsHandler` - represented as a typealias: `WsHandler =  (Request) -> WsConsumer`. This is responsible for matching an HTTP request to a websocket.
1. `WsConsumer` - represented as a typealias: `WsConsumer = (WebSocket) -> Unit`. This function is called on connection of a websocket and allow the API user to react to events coming from the connected websocket.
1. `WsMessage` - a message which is sent or received on a websocket. This message can take advantage of the typesafety accorded to other entities in http4k by using the Lens API. Just like the [**http4k**](https://github.com/http4k/http4k) HTTP message model, WsMessages are immutable data classes.
1. `WsFilter` - represented as a interface: `WsFilter = (WsConsumer) -> WsConsumer`. This allows for the decoration of `WsConsumers` to add pre or post matching behaviour in the same way as a standard `Filter`.

### Websocket as a Function
The simplest possible Websocket can be mounted as a `WsConsumer` function onto a server with:
```kotlin
{ ws: Websocket -> ws.send(WsMessage("hello")) }.asServer(Jetty(9000)).start()
```

### Mixing HTTP and Websocket services [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/howto/serve_websockets/example_polyhandler.kt)
Both Websockets and Http handlers in **http4k** are routed using a similar path-based API. We combine them into a single `PolyHandler` which can handle both `http://` and `ws://`, and then convert to a Server as usual:

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/howto/serve_websockets/example_polyhandler.kt"></script>

### Auto-marshalling Websockets messages [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/howto/serve_websockets/example_automarshalling.kt)
Using the standard Lens API, we can auto-convert Websocket messages on and off the wire. This example uses the Jackson for the marshalling:

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/howto/serve_websockets/example_automarshalling.kt"></script>

### Testing Websockets [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/howto/serve_websockets/example_testing.kt)
**http4k** provides Websockets that are both typesafe (via the Lens API), and testable. Both `WsHandlers` and `PolyHandlers` are convertible to a `WsClient` which provides a synchronous API for testing reactions to Websocket events in an offline environment.

In the below example, we have gone one step further - defining a contract test case and then providing 2 implementations of it - one for unit-testing (in memory), one using a server. [**http4k**](https://github.com/http4k/http4k) provides clients with an identical interface for both cases, meaning it's possible reuse the same test logic:

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/howto/serve_websockets/example_testing.kt"></script>
