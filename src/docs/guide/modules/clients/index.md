title: http4k Client Modules
description: Feature overview of the http4k-client modules

### Installation (Gradle)
**Java (for development only):** ```compile group: "org.http4k", name: "http4k-core", version: "3.224.0"```

**Apache (Sync):** ```compile group: "org.http4k", name: "http4k-client-apache", version: "3.224.0"```

**Apache (Async):** ```compile group: "org.http4k", name: "http4k-client-apache-async", version: "3.224.0"```

**Jetty (Sync + Async):** ```compile group: "org.http4k", name: "http4k-client-jetty", version: "3.224.0"```

**OkHttp (Sync + Async):** ```compile group: "org.http4k", name: "http4k-client-okhttp", version: "3.224.0"```

**Websocket:** ```compile group: "org.http4k", name: "http4k-client-websocket", version: "3.224.0"```

### HTTP
Supported HTTP client adapter APIs are wrapped to provide an `HttpHandler` interface in 1 LOC.

Activate streaming mode by passing a `BodyMode` (default is non-streaming).

These examples are for the Apache HTTP client, but the API is similar for the others:

#### Code [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/modules/clients/example_http.kt)
<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/modules/clients/example_http.kt"></script>

Additionally, all HTTP client adapter modules allow for custom configuration of the relevant underlying client. Async-supporting clients implement the `AsyncHttpClient` interface can be passed a callback.

### Websocket
http4k supplies both blocking and non-blocking Websocket clients. The former is perfect for integration testing purposes, and as it uses the same interface `WsClient` as the in-memory test client (`WsHandler.testWsClient()`) it is simple to write unit tests which can then be reused as system tests by virtue of swapping out the client.

#### Code [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/modules/clients/example_websocket.kt)
<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/modules/clients/example_websocket.kt"></script>
