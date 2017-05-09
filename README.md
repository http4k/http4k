# http4k

[![coverage](https://coveralls.io/repos/http4k/http4k/badge.svg?branch=master)](https://coveralls.io/github/http4k/http4k?branch=master)
[![kotlin](https://img.shields.io/badge/kotlin-1.1.2-blue.svg)](http://kotlinlang.org)
[![build status](https://travis-ci.org/http4k/http4k.svg?branch=master)](https://travis-ci.org/http4k/http4k)
[![bintray version](https://api.bintray.com/packages/http4k/maven/http4k-core/images/download.svg)](https://bintray.com/http4k/maven/http4k-core/_latestVersion)

http4k is an HTTP toolkit written in Kotlin that provides the ability to serve and consume HTTP services in a sensible and consistent way. 
It consists of a core library `http4k-core` providing a base HTTP implementation + a number of abstractions for various functionalities (such as 
servers, clients, templating etc) that are then provided in a set of optional add-on libraries.

The core axioms of the toolkit are:

* *Application as a Function:* Based on the famous [Twitter paper](https://monkey.org/~marius/funsrv.pdf), all HTTP services can be composed of 2 types of simple function:
    * HttpHandler: `typealias HttpHandler = (Request) -> Response` - provides a remote call for processing a `Request`. 
    * Filter: `interface Filter : (HttpHandler) -> HttpHandler` - adds pre or post processing to a `HttpHandler`. These filters are composed to make stacks of reusable behaviour that can then 
    be applied to a `HttpHandler`.
* *Immutablility:* All entities in the library are immutable unless their function explicitly disallows this.
* *Symmetric:* The `HttpHandler` interface is identical for both HTTP services and clients. This allows for simple offline testability of applications, as well as plugging together 
of services without HTTP container being required.
* *Dependency-lite:* The `http-core` module has ZERO dependencies. Add-on modules only have dependencies required for specific implementation.
* *Modularity:* Common behaviours are abstracted into the `http4k-core` module. Current add-ons cover:
   * Clients: [ApacheHttpClient](#user-content-module-http4k-client-library)
   * Servers: [Jetty, Netty](#user-content-module-http4k-server-library)
   * Message formats: [Argo JSON, Jackson JSON](#user-content-module-http4k-format-library)
   * Templating: [Handlebars](#user-content-module-http4k-template-library)


## Module: http4k-core
Gradle: ```compile group: "org.http4k", name: "http4k", version: "0.17.0"```

The core module has 0 dependencies and provides the following:
* Immutable versions of the HTTP spec objects (Request, Response, Cookies etc).
* HTTP handler and filter abstraction which models services as simple, composable functions.
* Simple routing implementation, plus `HttpHandlerServlet` to enable plugging into any Servlet engine. 
* Type-safe Lens mechanism for decomposition and composition of HTTP message entities.
* Abstractions for Servers, Clients, messasge formats, Templating etc.

### Getting started

#### HttpHandlers (`typealias HttpHandler = (Request) -> Response`)
Applications are modelled as functions. Note that we don't need a container to test this out:
```kotlin
val handler = { request: Request -> Response(OK).body("Hello, ${request.query("name")}!") }
val get = get("/").query("name", "John Doe")
val response = app(get)

println(response.status)
println(response.bodyString())
```

To mount the HttpHandler in a container, the can simply be converted to a Servlet by calling ```handler.asServlet()```

### Filters (`interface Filter : (HttpHandler) -> HttpHandler`)
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

Reekwest comes with basic routing:

```kotlin
routes(
    GET to "/hello/{name:*}" by { request: Request -> ok().body("Hello, ${request.path("name")}!") },
    POST to "/fail" by { request: Request -> Response(INTERNAL_SERVER_ERROR) }
).startJettyServer()
```

### Other features

Creates `curl` command for a given request:

```kotlin
val curl = post("http://httpbin.org/post").body(listOf("foo" to "bar").toBody()).toCurl()
// curl -X POST --data "foo=bar" "http://httpbin.org/post"
```

## Module: http4k-server-{library}
Gradle: ```compile group: "org.http4k", name: "http4k-server-<jetty|netty>", version: "0.17.0"```

Server modules provide extension functions to HttpHandler to mount them into the specified container:

```kotlin
{ _: Request -> Response(OK).body("Hello World") }.asJettyServer(8000).start().block()
```

## Module: http4k-client-{library}
Gradle: ```compile group: "org.http4k", name: "http4k-client-apache", version: "0.17.0"```

Client modules provide extension functions to HttpHandler to mount them into the specified container:

```kotlin
val client = HttpClients.createDefault().asHttpHandler()
val request = get("http://httpbin.org/get").query("location", "John Doe")
val response = client(request)
println(response.status)
println(response.bodyString())
```

## Module: http4k-format-{library}
Gradle: ```compile group: "org.http4k", name: "http4k-format-<argo|jackson>", version: "0.17.0"```

coming soon...

## Module: http4k-template-{library}
Gradle: ```compile group: "org.http4k", name: "http4k-template-handlebars", version: "0.17.0"```

coming soon...

## Module: http4k-contract
Gradle: ```compile group: "org.http4k", name: "http4k-contract", version: "0.17.0"```

