# http4k

[![coverage](https://coveralls.io/repos/http4k/http4k/badge.svg?branch=master)](https://coveralls.io/github/http4k/http4k?branch=master)
[![kotlin](https://img.shields.io/badge/kotlin-1.1.2-blue.svg)](http://kotlinlang.org)
[![build status](https://travis-ci.org/http4k/http4k.svg?branch=master)](https://travis-ci.org/http4k/http4k)
[![bintray version](https://api.bintray.com/packages/http4k/maven/http4k-core/images/download.svg)](https://bintray.com/http4k/maven/http4k-core/_latestVersion)

**http4k** is an HTTP toolkit written in [Kotlin](https://kotlinlang.org/) that allows serving and consuming HTTP services in a sensible and consistent way.

It consists of a core library `http4k-core` providing a base HTTP implementation + a number of abstractions for various functionalities (such as 
servers, clients, templating etc) provided as optional add-on libraries.

The principles of the toolkit are:

* **Application as a Function:** Based on the Twitter paper ["Your Server as a Function"](https://monkey.org/~marius/funsrv.pdf), all HTTP services can be composed of 2 types of simple function:
    * *HttpHandler:* `(Request) -> Response` - provides a remote call for processing a Request. 
    * *Filter:* `(HttpHandler) -> HttpHandler` - adds pre or post processing to a `HttpHandler`. These filters are composed to make stacks of reusable behaviour that can then 
    be applied to a `HttpHandler`.
* **Immutablility:** All entities in the library are immutable unless their function explicitly disallows this.
* **Symmetric:** The `HttpHandler` interface is identical for both HTTP services and clients. This allows for simple offline testability of applications, as well as plugging together 
of services without HTTP container being required.
* **Dependency-lite:** The `http-core` module has ZERO dependencies. Add-on modules only have dependencies required for specific implementation.
* **Modularity:** Common behaviours are abstracted into the `http4k-core` module. Current add-ons cover:
   * *Clients:* [ApacheHttpClient](#user-content-module-http4k-client-library)
   * *Servers:* [Jetty, Netty](#user-content-module-http4k-server-library)
   * *Message formats:* [Argo JSON, Jackson JSON](#user-content-module-http4k-format-library)
   * *Templating:* [Handlebars](#user-content-module-http4k-template-library)

# Getting started
This simple example demonstates how to serve and consume HTTP services using http4k. 

To install, add these dependencies to your **Gradle** file:
```groovy
dependencies {
    compile group: "org.http4k", name: "http4k-core", version: "0.17.0"
    compile group: "org.http4k", name: "http4k-server-jetty", version: "0.17.0"
    compile group: "org.http4k", name: "http4k-client-apache", version: "0.17.0"
}
```

The following creates a simple endpoint to as Jetty server, then starts, queries, and stops it.

```kotlin
import org.http4k.client.ApacheHttpClient
import org.http4k.core.Request
import org.http4k.core.Request.Companion.get
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.server.asJettyServer

fun main(args: Array<String>) {

    val app = { request: Request -> Response(OK).body("Hello, ${request.query("name")}!") }

    val jettyServer = app.asJettyServer(9000)

    jettyServer.start()

    val request = get("http://localhost:9000").query("name", "John Doe")

    val client = ApacheHttpClient()

    println(client(request))

    jettyServer.stop()
}
```

# Modules

## Core Module
**Gradle:** ```compile group: "org.http4k", name: "http4k-core", version: "0.17.0"```

The core module has ZERO dependencies and provides the following:
* Immutable versions of the HTTP spec objects (Request, Response, Cookies etc).
* HTTP handler and filter abstraction which models services as simple, composable functions.
* Simple routing implementation, plus `HttpHandlerServlet` to enable plugging into any Servlet engine. 
* Type-safe [Lens](https://www21.in.tum.de/teaching/fp/SS15/papers/17.pdf) mechanism for destructuring and construction of HTTP message entities.
* Abstractions for Servers, Clients, messasge formats, Templating etc.

#### HttpHandlers 
In **http4k**, an HTTP service or handler is just a typealias of a simple function:
`typealias HttpHandler = (Request) -> Response`

First described in this Twitter paper ["Your Server as a Function"](https://monkey.org/~marius/funsrv.pdf), this abstraction allows us lots of 
flexibility in a language like Kotlin, since the conceptual barrier to service construction is reduced to effectively nil. Here is the simplest example 
- note that we don't need any special infrastructure to create an HttpHandler, neither do we need to launch a real HTTP container to exercise it:
```kotlin
val handler = { request: Request -> Response(OK).body("Hello, ${request.query("name")}!") }
val get = get("/").query("name", "John Doe")
val response = app(get)

println(response.status)
println(response.bodyString())
```

To mount the HttpHandler in a container, the can simply be converted to a Servlet by calling ```handler.asServlet()```

### Filters
 `interface Filter : (HttpHandler) -> HttpHandler`
Filters add extra processing to either the Request or Response and compose together to create reusable stacks of behaviour. For example, 
to add Basic Auth and latency reporting to a service:
```kotlin
val handler = { _: Request -> Response(OK) }

val myFilter = Filter {
    nextHandler -> {
        request: Request -> 
            val start = System.currentTimeMillis()
            val response = next(it)
            val latency = System.currentTimeMillis() - start
            println("I took $latency ms")
            response
    }
}
val latencyAndBasicAuth: HttpHandler = ServerFilters.BasicAuth("my realm", "user", "password").then(myFilter)
val app: HttpHandler = latencyAndBasicAuth.then(handler)
```

### Routing
Basic routing for mapping URL patterns to HttpHandlers:
```kotlin
routes(
    GET to "/hello/{name:*}" by { request: Request -> Response(OK).body("Hello, ${request.path("name")}!") },
    POST to "/fail" by { request: Request -> Response(INTERNAL_SERVER_ERROR) }
).startJettyServer()
```

### Typesafe parameter destructuring/construction of HTTP messages with Lenses

A [Lens](https://www21.in.tum.de/teaching/fp/SS15/papers/17.pdf) is a bi-directional entity which can be used to either get or set a particular value on/from an HTTP message. Http4k provides a DSL to configure these lenses 
to target particular parts of the message, whilst at the same time specifying the requirement for those parts (i.e. mandatory or optional). Some examples of declarations are:

```kotlin
val requiredQuery = Query.required("myQueryName")
val optionalHeader = Header.int().optional("Content-Length")
val requiredJsonBody = Body.string(APPLICATION_JSON)

data class CustomType(val value: String)
val requiredCustomQuery = Query.map(::CustomType, { it.value }).required("myCustomType")
```

To use the Lens, simply apply it to an HTTP message, passing the value if we are modifying the message:

```kotlin
val optionalHeader: Int? = optionalHeader(get(""))
val customType: CustomType = requiredCustomQuery(get("?myCustomType=someValue"))
val modifiedRequest: Request = get("").with(requiredQuery to customType.value)
```

### Other features
Creates `curl` command for a given request:

```kotlin
val curl = post("http://httpbin.org/post").body(listOf("foo" to "bar").toBody()).toCurl()
// curl -X POST --data "foo=bar" "http://httpbin.org/post"
```

## Server Modules
**Gradle** (Jetty): ```compile group: "org.http4k", name: "http4k-server-jetty", version: "0.17.0"```
**Gradle** (Netty): ```compile group: "org.http4k", name: "http4k-server-netty", version: "0.17.0"```

Server modules provide extension functions to HttpHandler to mount them into the specified container:

```kotlin
{ _: Request -> Response(OK).body("Hello World") }.asJettyServer(8000).start().block()
```

## Client Modules
**Gradle:** ```compile group: "org.http4k", name: "http4k-client-apache", version: "0.17.0"```

Client modules provide extension functions to HttpHandler to mount them into the specified container:

```kotlin
val client = HttpClients.createDefault().asHttpHandler()
val request = get("http://httpbin.org/get").query("location", "John Doe")
val response = client(request)
println(response.status)
println(response.bodyString())
```

## Message Format Modules
**Gradle:** (Argo) ```compile group: "org.http4k", name: "http4k-format-argo", version: "0.17.0"```
**Gradle:** (Jackson) ```compile group: "org.http4k", name: "http4k-format-jackson", version: "0.17.0"```

coming soon...

## Templating Modules
**Gradle:** ```compile group: "org.http4k", name: "http4k-template-handlebars", version: "0.17.0"```

coming soon...

## Contracts Module
**Gradle:** ```compile group: "org.http4k", name: "http4k-contract", version: "0.17.0"```

