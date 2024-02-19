title: http4k How-to: Testing Websocket Clients
description: Recipes for testing websocket clients with http4k

### Gradle setup

```kotlin
dependencies {
    implementation(platform("org.http4k:http4k-bom:5.13.7.0"))
    implementation("org.http4k:http4k-server-undertow")
    implementation("org.http4k:http4k-client-websocket")
}
```

### WebSocket as a (Symmetric) Function

**http4k** provides a symmetric Websocket handler to simplify the testing of WebSocket clients.

The `SymmetricWsHandler` is just like the `HttpHandler` in that it can be implemented by an in-memory server or a client opening a connection to a real server.

### Example Client Main Method and Test [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/howto/testing_websocket_clients/example.kt)

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/howto/testing_websocket_clients/example.kt"></script>
