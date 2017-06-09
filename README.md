# http4k

[![coverage](https://coveralls.io/repos/http4k/http4k/badge.svg?branch=master)](https://coveralls.io/github/http4k/http4k?branch=master)
[![build status](https://travis-ci.org/http4k/http4k.svg?branch=master)](https://travis-ci.org/http4k/http4k)
[![Download](https://api.bintray.com/packages/http4k/maven/http4k-core/images/download.svg)](https://bintray.com/http4k/maven/http4k-core/_latestVersion)
[![GitHub license](https://img.shields.io/badge/license-Apache%20License%2.2.1-blue.svg?style=flat)](http://www.apache.org/licenses/LICENSE-2.0)
[![kotlin](https://img.shields.io/badge/kotlin-1.1.2-blue.svg)](http://kotlinlang.org)
[![codebeat badge](https://codebeat.co/badges/5b369ed4-af27-46f4-ad9c-a307d900617e)](https://codebeat.co/projects/github-com-http4k-http4k-master)
[![Gitter](https://img.shields.io/badge/gitter-join%20chat-1dce73.svg)](https://gitter.im/http4k/http4k)
[![Awesome Kotlin Badge](https://kotlin.link/awesome-kotlin.svg)](https://kotlin.link)

**http4k** is an HTTP toolkit written in [Kotlin](https://kotlinlang.org/) that enables the serving and consuming of HTTP services in a functional and consistent way.

It consists of a core library `http4k-core` providing a base HTTP implementation and a number of abstractions for various functionalities (such as 
server backends, clients, templating etc) that are provided as optional add-on libraries.

The principles of the toolkit are:

* **Application as a Function:** Based on the Twitter paper ["Your Server as a Function"](https://monkey.org/~marius/funsrv.pdf), all HTTP services can be composed of 2 types of simple function:
    * *HttpHandler:* `(Request) -> Response` - provides a remote call for processing a Request. 
    * *Filter:* `(HttpHandler) -> HttpHandler` - adds Request/Response pre/post processing. These filters are composed to make stacks of reusable behaviour that can then 
    be applied to an `HttpHandler`.
* **Immutablility:** All entities in the library are immutable unless their function explicitly disallows this.
* **Symmetric:** The unified `Http` API is identical for both HTTP services and clients. This allows for simple offline testability of applications, as well as plugging together 
of services without HTTP container being required.
* **Dependency-lite:** The [`http4k-core`](https://github.com/http4k/http4k/wiki/Core-Module) module has ZERO dependencies. Add-on modules only have dependencies required for specific implementation.
* **Testability** Built by **TDD** enthusiasts, so supports **super-easy** mechanisms for both In and Out of Container testing of:
    * individual endpoints
    * applications
    * full suites of microservices

## Example
This quick example is designed to convey the simplicity & features of **http4k**. There is also a [quickstart](https://github.com/http4k/http4k/wiki/Quickstart) on the wiki for the simplest possible starting point.

To install, add these dependencies to your **Gradle** file:
```groovy
dependencies {
    compile group: "org.http4k", name: "http4k-core", version: "2.2.1"
    compile group: "org.http4k", name: "http4k-server-jetty", version: "2.2.1"
    compile group: "org.http4k", name: "http4k-client-apache", version: "2.2.1"
}
```

This "hello world" style example demonstrates how to serve and consume HTTP services with dynamic routing:
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
        "/ping" to GET bind { _: Request -> Response(OK).body("pong!") },
        "/greet/{name}" to GET bind { req: Request ->
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
//    date: Thu, 08 Jun 2017 13:01:13 GMT
//    expires: 0
//    server: Jetty(9.3.16.v20170120)
//
//    hello Bob
}
```

## Module feature overview
* [Core:](https://github.com/http4k/http4k/wiki/Core-Module) 
    * Base HTTP handler and **immutable HTTP message** objects, cookie handling. 
    * Commonly used HTTP functionalities provided as reusable Filters (caching, debugging, **Zipkin request tracing**)
    * **Path-based routing**, including nestable contexts
    * **Typesafe HTTP message construction/desconstruction** using Lenses
    * **Static file-serving** capability with **Caching and Hot-Reload** 
    * Servlet implementation to allow **zero-dependency plugin to any Servlet container**
    * Core abstraction APIs implemented by the other modules 
* [Client:](https://github.com/http4k/http4k/wiki/HTTP-Client-Modules) 
    * **Single LOC** HTTP client adapters 
        * **Apache**
        * **OkHttp**
* [Server:](https://github.com/http4k/http4k/wiki/Server-Backend-Modules)
    * **Single LOC** server backend spinup for:
        * **Jetty**
        * **Netty**
        * **Undertow**
    * API design allows for plugging into configurable instances of each
* **BETA!** [Contracts:](https://github.com/http4k/http4k/wiki/Contract-Module) 
   * Definite **Typesafe** HTTP contracts, defining required and optional path/query/header/bodies
   * **Typesafe** path matching
   * **Auto-validation** of incoming requests == **zero boilerplate validation code**
   * Self-documenting for all routes - eg. Built in support for live **Swagger** description endpoints including **JSON Schema** model breakdown. 
* [Templating:](https://github.com/http4k/http4k/wiki/Templating-Modules) 
    * **Pluggable** templating system support for:
        * Handlebars 
    * Caching and **Hot-Reload** template support
* [Message formats:](https://github.com/http4k/http4k/wiki/Message-Format-Modules) 
    * Consistent API provides first class support for marshalling JSON to/from HTTP messages for:
        * **Jackson** -includes support for **fully automatic marshalling of Data classes**)
        * **Gson**
        * **Argo**

## See it in action:
* [Cookbook example code](https://github.com/http4k/http4k/tree/master/src/test/kotlin/cookbook)
* [Todo backend (simple version)](https://github.com/http4k/http4k-todo-backend)
* [Todo backend (typesafe contract version)](https://github.com/http4k/http4k-contract-todo-backend)
* [TDD'd example application](https://github.com/http4k/http4k-contract-example-app)
* [Stage-by-stage example of development process (London TDD style)](https://github.com/http4k/http4k/tree/master/src/test/kotlin/worked_example)

## Further reading
* Much more detailed documentation and examples are available on the project [wiki](https://github.com/http4k/http4k/wiki)

## Acknowledgments
* [Dan Bodart](https://twitter.com/DanielBodart)'s [utterlyidle](https://github.com/bodar/utterlyidle)
* [Ivan Moore](https://twitter.com/ivanrmoore) for pairing on "BarelyMagical", a 50-line wrapper around utterlyidle to allow "Server as a Function"

