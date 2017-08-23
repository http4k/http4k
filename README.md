<a href="https://http4k.org"><img src="https://www.http4k.org/img/logo_1100x200_blue_on_white.png"/></a>

<span class="github">
<a href="https://travis-ci.org/http4k/http4k"><img alt="build status" src="https://travis-ci.org/http4k/http4k.svg?branch=master"></a>
<a href="https://coveralls.io/github/http4k/http4k?branch=master"><img alt="coverage" src="https://coveralls.io/repos/http4k/http4k/badge.svg?branch=master"></a>
<a href="http//www.apache.org/licenses/LICENSE-2.0"><img alt="GitHub license" src="https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat"></a>
<a href="https://bintray.com/http4k/maven/http4k-core/_latestVersion"><img alt="Download" src="https://api.bintray.com/packages/http4k/maven/http4k-core/images/download.svg"></a>
<a href="http://kotlinlang.org"><img alt="kotlin" src="https://img.shields.io/badge/kotlin-1.1.4-blue.svg"></a>
<a href="https://codebeat.co/projects/github-com-http4k-http4k-master"><img alt="codebeat badge" src="https://codebeat.co/badges/5b369ed4-af27-46f4-ad9c-a307d900617e"></a>
<a href="https://kotlin.link"><img alt="Awesome Kotlin Badge" src="https://kotlin.link/awesome-kotlin.svg"></a>
<a href="https://gitter.im/http4k/http4k"><img alt="Gitter" src="https://img.shields.io/badge/gitter-join%20chat-1dce73.svg"></a>
</span>

**http4k** is an HTTP toolkit written in [Kotlin](https://kotlinlang.org/) that enables the serving and consuming of HTTP services in a functional and consistent way.

It consists of a core library `http4k-core` providing a base HTTP implementation + a number of abstractions for various functionalities (such as 
servers, clients, templating etc) that are provided as optional add-on libraries.

The principles of the toolkit are:

* **Application as a Function:** Based on the Twitter paper ["Your Server as a Function"](https://monkey.org/~marius/funsrv.pdf), all HTTP services can be composed 
of 2 types of simple function:
    * *HttpHandler:* `(Request) -> Response` - provides a remote call for processing a Request. 
    * *Filter:* `(HttpHandler) -> HttpHandler` - adds Request/Response pre/post processing. These filters are composed to make stacks of reusable behaviour that can 
    then be applied to an `HttpHandler`.
* **Immutablility:** All entities in the library are immutable unless their function explicitly disallows this.
* **Symmetric:** The `HttpHandler` interface is identical for both HTTP services and clients. This allows for simple offline testability of applications, as well as 
plugging together of services without HTTP container being required.
* **Dependency-lite:** Apart the from Kotlin StdLib, `http4k-core` module has ZERO dependencies and weighs in at ~500kb. Add-on modules only have dependencies required for specific implementation.
* **Testability** Built by **TDD** enthusiasts, so supports **super-easy** mechanisms for both In and Out of Container testing of:
    * individual endpoints
    * applications
    * full suites of microservices
* **Modularity:** Common behaviours are abstracted into the `http4k-core` module. Current add-ons cover:
    * Pluggable HTTP client adapters for Apache and OkHttp
    * Pluggable Server backends: Single LOC Server spinup for Jetty, Netty, Undertow and SunHttp
    * Typesafe, auto-validating, self-documenting (via Swagger) contracts for HTTP services
    * HTTP message adapters for Argo JSON, Gson JSON and Jackson JSON (includes auto-marshalling)
    * Templating support: Caching and Hot-Reload engine support for Handlebars
    * AWS request signing: super-simple interactions with AWS services
    * Testing: Selenium WebDriver implementation for lightning fast, browserless testing of **http4k** apps
    * Testing: Hamkrest Matchers for **http4k** objects

## Module feature overview
* [Core:](https://http4k.org/guide/modules/core) 
    * Base HTTP handler and **immutable HTTP message** objects, cookie handling. 
    * Commonly used HTTP functionalities provided as reusable Filters (caching, debugging, **Zipkin request tracing**)
    * **Path-based routing**, including nestable contexts
    * **Typesafe HTTP message construction/desconstruction** using Lenses
    * **Static file-serving** capability with **Caching and Hot-Reload** 
    * Servlet implementation to allow **
    -dependency plugin to any Servlet container**
    * Launch applications in **1LOC** with an embedded **SunHttp** server backend (recommended for development use only)
    * Core abstraction APIs implemented by the other modules 
* [Client:](https://http4k.org/guide/modules/clients) 
    * **1LOC** HTTP client adapters 
        * **Apache**
        * **OkHttp**
* [Server:](https://http4k.org/guide/modules/servers)
    * **1LOC** server backend spinup for:
        * **Jetty**
        * **Netty**
        * **Undertow**
        * **SunHttp** (bundled with `http4k-core`)
    * API design allows for plugging into configurable instances of each
* [Contracts:](https://http4k.org/guide/modules/contracts) 
    * Definite **Typesafe** HTTP contracts, defining required and optional path/query/header/bodies
    * **Typesafe** path matching
    * **Auto-validation** of incoming requests == **zero boilerplate validation code**
    * Self-documenting for all routes - eg. Built in support for live **Swagger** description endpoints including **JSON Schema** model breakdown. 
* [Templating:](https://http4k.org/guide/modules/templating) 
    * **Pluggable** templating system support for:
        * **Handlebars** 
        * **Pebble**
        * **Thymeleaf**
    * Caching and **Hot-Reload** template support
* [Message formats:](https://http4k.org/guide/modules/message_formats) 
    * Consistent API provides first class support for marshalling JSON to/from HTTP messages for:
        * **Jackson** - includes support for **fully [automatic marshalling](https://http4k.org/guide/modules/message_formats/#auto-marshalling-capabilities) of Data classes**)
        * **Gson** - includes support for **fully [automatic marshalling](https://http4k.org/guide/modules/message_formats/#auto-marshalling-capabilities) of Data classes**)
        * **Argo** - lightweight Java JSON API with zero dependencies.
* [AWS:](https://http4k.org/guide/modules/aws) 
    * Client filter to allow super-simple interaction with AWS services (via request signing)
* [WebDriver:](https://http4k.org/guide/modules/webdriver) 
    * Ultra-lightweight Selenium WebDriver implementation for **http4k** application.
* [Hamkrest:](https://http4k.org/guide/modules/hamkrest) 
    * A set of Hamkrest matchers for testing **http4k** Request and Response messages.
    
## Example
This quick example is designed to convey the simplicity & features of **http4k**. See also the [quickstart](https://http4k.org/quickstart/) for the simplest possible starting point.

To install, add these dependencies to your **Gradle** file:
```groovy
dependencies {
    compile group: "org.http4k", name: "http4k-core", version: "2.21.1"
    compile group: "org.http4k", name: "http4k-server-jetty", version: "2.21.1"
    compile group: "org.http4k", name: "http4k-client-okhttp", version: "2.21.1"
}
```

This "Hello World" style example demonstrates how to serve and consume HTTP services with dynamic routing:
```kotlin
package cookbook

import org.http4k.client.OkHttp
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.filter.CachingFilters
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.routes
import org.http4k.server.Jetty
import org.http4k.server.asServer

fun main(args: Array<String>) {
    // we can bind HttpHandlers (which are just functions from  Request -> Response) to paths/methods to create a Route,
    // then combine many Routes together to make another HttpHandler
    val app: HttpHandler = routes(
        "/ping" bind GET to { _: Request -> Response(OK).body("pong!") },
        "/greet/{name}" bind GET to { req: Request ->
            val path: String? = req.path("name")
            Response(OK).body("hello ${path ?: "anon!"}")
        }
    )

    // call the handler in-memory without spinning up a server
    val inMemoryResponse: Response = app(Request(GET, "/greet/Bob"))
    println(inMemoryResponse)

// Produces:
//    HTTP/1.1 200 OK
//
//
//    hello Bob

    // this is a Filter - it performs pre/post processing on a request or response
    val timingFilter = Filter {
        next: HttpHandler ->
        {
            request: Request ->
            val start = System.currentTimeMillis()
            val response = next(request)
            val latency = System.currentTimeMillis() - start
            println("Request to ${request.uri} took ${latency}ms")
            response
        }
    }

    // we can "stack" filters to create reusable units, and then apply them to an HttpHandler
    val compositeFilter = CachingFilters.Response.NoCache().then(timingFilter)
    val filteredApp: HttpHandler = compositeFilter.then(app)

    // only 1 LOC to mount an app and start it in a container
    filteredApp.asServer(Jetty(9000)).start()

    // HTTP clients are also HttpHandlers!
    val client: HttpHandler = OkHttp()

    val networkResponse: Response = client(Request(GET, "http://localhost:9000/greet/Bob"))
    println(networkResponse)

// Produces:
//    Request to /api/greet/Bob took 1ms
//    HTTP/1.1 200
//    cache-control: private, must-revalidate
//    content-length: 9
//    date: Thu, 08 Jun 2.21.13:01:13 GMT
//    expires: 0
//    server: Jetty(9.3.16.v2.21.120)
//
//    hello Bob
}
```

## Acknowledgments

* [Dan Bodart](https://twitter.com/DanielBodart)'s [utterlyidle](https://github.com/bodar/utterlyidle)
* [Ivan Moore](https://twitter.com/ivanrmoore) for pairing on "BarelyMagical", a 50-line wrapper around utterlyidle to allow "Server as a Function"

