title: http4k Server Modules
description: Feature overview of the http4k-server modules, covering Server backends

### Installation (Gradle)

```groovy
// Apache v5: 
implementation group: "org.http4k", name: "http4k-server-apache", version: "4.25.4.1"

// Apache v4: 
implementation group: "org.http4k", name: "http4k-server-apache4", version: "4.25.4.1"

// Jetty: 
implementation group: "org.http4k", name: "http4k-server-jetty", version: "4.25.4.1"

// Ktor CIO: 
implementation group: "org.http4k", name: "http4k-server-ktorcio", version: "4.25.4.1"

// Ktor Netty: 
implementation group: "org.http4k", name: "http4k-server-ktornetty", version: "4.25.4.1"

// Netty: 
implementation group: "org.http4k", name: "http4k-server-netty", version: "4.25.4.1"

// Ratpack: 
implementation group: "org.http4k", name: "http4k-server-ratpack", version: "4.25.4.1"

// Undertow: 
implementation group: "org.http4k", name: "http4k-server-undertow", version: "4.25.4.1"

// SunHttp (for development only): 
implementation group: "org.http4k", name: "http4k-core", version: "4.25.4.1"
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
