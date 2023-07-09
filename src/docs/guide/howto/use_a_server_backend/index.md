title: http4k How-to: Use a Server backend
description: Recipes for using http4k with the various Server backends

This example shows how to both serve an application HttpHandler using an embedded HTTP server and to query it using an HTTP client. All server-backend implementations are launched in an identical manner (in 1LOC) using implementations of the `ServerConfig` interface - and a base implementation of this interface is provided for each server backend.

Alternatively, any http4k application can be mounted into any Servlet container using the `asServlet()` extension method. This is the mechanism used in the Jetty implementation.

### Gradle setup

```kotlin
dependencies {
    implementation(platform("org.http4k:http4k-bom:5.3.0.0"))
    implementation("org.http4k:http4k-core")
    implementation("org.http4k:http4k-client-apache")
    implementation("org.http4k:http4k-server-jetty")
}
```

### Code [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/howto/use_a_server_backend/example.kt)

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/howto/use_a_server_backend/example.kt"></script>
