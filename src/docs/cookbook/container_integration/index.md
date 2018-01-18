title: http4k Container Integration
description: Recipes for using http4k to with the various Server backends

This example shows how to both how to serve an application HttpHandler using an embedded an HTTP server and to query it using an HTTP client. All server-backend implementations are launched in an identical manner (in 1LOC) using implementations of the `ServerConfig` interface - and a base implementation of this interface is provided for each server backend.

Alternatively, any http4k application can be mounted into any Servlet container using the `asServlet()` extension method. This is the mechanism used in the Jetty implementation.

### Gradle setup
```
    compile group: "org.http4k", name: "http4k-core", version: "3.11.1"
    compile group: "org.http4k", name: "http4k-client-apache", version: "3.11.1"
    compile group: "org.http4k", name: "http4k-server-jetty", version: "3.11.1"
```

### Code [<img class="octocat" src="/img/octocat-32.png"/>](https://github.com/http4k/http4k/blob/master/src/docs/cookbook/container_integration/example.kt)
<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/cookbook/container_integration/example.kt"></script>
