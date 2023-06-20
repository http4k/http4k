title: http4k Server Modules
description: Feature overview of the http4k-server modules, covering Server backends

### Installation (Gradle)

```kotlin
dependencies {
    implementation(platform("org.http4k:http4k-bom:5.0.0.0"))
    // Apache v5: 
    implementation("org.http4k:http4k-server-apache")

    // Apache v4: 
    implementation("org.http4k:http4k-server-apache4")

    // Jetty & JettyLoom: 
    implementation("org.http4k:http4k-server-jetty")

    // Helidon Nima (Loom): 
    implementation("org.http4k:http4k-server-helidon")

    // Ktor CIO: 
    implementation("org.http4k:http4k-server-ktorcio")

    // Ktor Netty: 
    implementation("org.http4k:http4k-server-ktornetty")

    // Netty: 
    implementation("org.http4k:http4k-server-netty")

    // Ratpack: 
    implementation("org.http4k:http4k-server-ratpack")

    // Undertow: 
    implementation("org.http4k:http4k-server-undertow")
    
    // Java WebSocket:
    implementation("org.http4k:http4k-server-websocket")

    // SunHttp & SunHttpLoom (for development only): 
    implementation("org.http4k:http4k-core")
}
```

### About
Server-backend modules provide a consistent API to mount HttpHandlers into the specified container in 1 LOC, by 
simply passing it to the relevant `ServerConfig` implementation (in this case `Jetty`):

#### Code [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/reference/servers/example_http.kt)

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/reference/servers/example_http.kt"></script>

### Customisation
Each of the server backends implement an interface `ServerConfig`, which is written with sensible defaults for the server in questions, 
but is also designed to be used as a starting point for tweaking to API user needs. To customize, simply use the relevant `ServerConfig` 
class as a starting point and reimplement as required. See the how-to guides for an example of this in use.
