title: http4k Client Modules
description: Feature overview of the http4k-client modules

### Installation (Gradle)

```kotlin
dependencies {
    implementation(platform("org.http4k:http4k-bom:5.7.4.0"))
    
    // Java (for development only):
    implementation("org.http4k:http4k-core")
    
    // Apache v5 (Sync): 
    implementation("org.http4k:http4k-client-apache")
    
    // Apache v4 (Sync): 
    implementation("org.http4k:http4k-client-apache4")
    
    // Apache v5 (Async): 
    implementation("org.http4k:http4k-client-apache-async")
    
    // Apache v4 (Async): 
    implementation("org.http4k:http4k-client-apache4-async")
    
    // Fuel (Sync + Async): 
    implementation("org.http4k:http4k-client-fuel")
    
    // Jetty (Sync + Async + WebSocket): 
    implementation("org.http4k:http4k-client-jetty")
    
    // OkHttp (Sync + Async): 
    implementation("org.http4k:http4k-client-okhttp")
    
    // Websocket: 
    implementation("org.http4k:http4k-client-websocket")
}
```

### HTTP
Supported HTTP client adapter APIs are wrapped to provide an `HttpHandler` interface in 1 LOC.

Activate streaming mode by passing a `BodyMode` (default is non-streaming).

These examples are for the Apache HTTP client, but the API is similar for the others:

#### Code [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/reference/clients/example_http.kt)

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/reference/clients/example_http.kt"></script>

Additionally, all HTTP client adapter modules allow for custom configuration of the relevant underlying client. Async-supporting clients implement the `AsyncHttpClient` interface can be passed a callback.

### Websocket
http4k supplies both blocking and non-blocking Websocket clients. The former is perfect for integration testing purposes, and as it uses the same interface `WsClient` as the in-memory test client (`WsHandler.testWsClient()`) it is simple to write unit tests which can then be reused as system tests by virtue of swapping out the client.

#### Code [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/reference/clients/example_websocket.kt)

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/reference/clients/example_websocket.kt"></script>

#### Testing Websockets with offline and online clients [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/reference/clients/TestingWebsockets.kt)

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/reference/clients/TestingWebsockets.kt"></script>
