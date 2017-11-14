title: http4k Container Integration
description: Recipes for using http4k to with the various Server backends

This example shows how to both how to serve an application HttpHandler using an embedded an HTTP server and to query it using an HTTP client. All server-backend implementations are launched in an identical manner (in 1LOC). Additionally, server instances can be customised using classes available in the implementation JARs - in the case of Jetty, the application is mounted using the `asServlet()` extension method into the Jetty container.

### Gradle setup
```
    compile group: "org.http4k", name: "http4k-core", version: "3.0.0"
    compile group: "org.http4k", name: "http4k-client-apache", version: "3.0.0"
    compile group: "org.http4k", name: "http4k-server-jetty", version: "3.0.0"
```

### Code
<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/cookbook/container_integration/example.kt"></script>
