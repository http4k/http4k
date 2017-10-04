### Installation (Gradle)
**SunHttp (for development only):** ```compile group: "org.http4k", name: "http4k-core", version: "2.29.4"```

**Jetty:** ```compile group: "org.http4k", name: "http4k-server-jetty", version: "2.29.4"```

**Netty:** ```compile group: "org.http4k", name: "http4k-server-netty", version: "2.29.4"```

**Undertow:** ```compile group: "org.http4k", name: "http4k-server-undertow", version: "2.29.4"```

### About
Server-backend modules provide a consistent API mount HttpHandlers into the specified container in 1 LOC, by simply passing a `ServerConfig` implementation (in this case `Jetty`):

```kotlin
{ request: Request -> Response(OK).body("Hello World") }.asServer(Jetty(8000)).start().block()
```
Alteratively, all server-backend modules allow for plugging **http4k** handlers into the relevant server API, which allows for custom Server configuration.
