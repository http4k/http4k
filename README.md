<div class="github">
<hr/>

<picture>
  <source 
    srcset="https://www.http4k.org/img/logo-readme-dark-mode.png" 
    media="(prefers-color-scheme: dark)">
  <img src="https://www.http4k.org/img/logo-readme.png">
</picture>

<hr/>

<a href="https://bintray.com/http4k/maven/http4k-core/_latestVersion"><img alt="Download" src="https://api.bintray.com/packages/http4k/maven/http4k-core/images/download.svg"></a>
<a href="https://travis-ci.org/http4k/http4k"><img alt="build status" src="https://travis-ci.org/http4k/http4k.svg?branch=master"/></a>
<a href="https://coveralls.io/github/http4k/http4k?branch=master"><img alt="coverage" src="https://coveralls.io/repos/http4k/http4k/badge.svg?branch=master"></a>
<a href="http//www.apache.org/licenses/LICENSE-2.0"><img alt="GitHub license" src="https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat"></a>
<a href="http://kotlinlang.org"><img alt="kotlin" src="https://img.shields.io/badge/kotlin-1.3-blue.svg"></a>
<a href="https://codebeat.co/projects/github-com-http4k-http4k-master"><img alt="codebeat badge" src="https://codebeat.co/badges/5b369ed4-af27-46f4-ad9c-a307d900617e"></a>
<a href="https://kotlin.link"><img alt="Awesome Kotlin Badge" src="https://kotlin.link/awesome-kotlin.svg"></a>
<a href="https://opencollective.com/http4k"><img alt="build status" src="https://opencollective.com/http4k/backers/badge.svg"></a>
<a href="https://opencollective.com/http4k"><img alt="build status" src="https://opencollective.com/http4k/sponsors/badge.svg"></a>

<hr/>

</div>

 [http4k] is a lightweight but fully-featured HTTP toolkit written in pure [Kotlin](https://kotlinlang.org/) that enables the serving and consuming of HTTP services in a functional and consistent way. [http4k] applications are *just* Kotlin functions which can be mounted into a running backend. For example, here's a simple echo server:
 
```kotlin
 val app: HttpHandler = { request: Request -> Response(OK).body(request.body) }
 val server = app.asServer(SunHttp(8000)).start()
```

[http4k] consists of a core library, `http4k-core`, providing a base HTTP implementation + a number of capability abstractions (such as servers, clients, templating, websockets etc). These capabilities are then implemented in add-on modules.

The principles of [http4k] are:

* **Application as a Function:** Based on the Twitter paper ["Your Server as a Function"](https://monkey.org/~marius/funsrv.pdf), all HTTP services can be composed 
of 2 types of simple function:
    * *HttpHandler:* `(Request) -> Response` - provides a remote call for processing a Request. 
    * *Filter:* `(HttpHandler) -> HttpHandler` - adds Request/Response pre/post processing. These filters are composed to make stacks of reusable behaviour that can 
    then be applied to an `HttpHandler`.
* **Immutability:** All entities in the library are immutable unless their function explicitly disallows this.
* **Symmetric:** The `HttpHandler` interface is identical for both HTTP services and clients. This allows for simple offline testability of applications, as well as 
plugging together of services without HTTP container being required.
* **Dependency-lite:** Apart the from Kotlin StdLib, `http4k-core` module has ZERO dependencies and weighs in at ~700kb. Add-on modules only have dependencies required for specific implementation.
* **Testability** Built by **TDD** enthusiasts, so supports **super-easy** mechanisms for both In and Out of Container testing of:
    * individual endpoints
    * applications
    * websockets
    * full suites of microservices

## Module feature overview
* [Core:](https://http4k.org/guide/modules/core) 
    * Base HTTP handler and **immutable HTTP message** objects, cookie handling. 
    * Commonly used HTTP functionalities provided as reusable Filters (caching, debugging, **Zipkin request tracing**)
    * **Path-based routing**, including nestable contexts
    * **Typesafe HTTP message construction/desconstruction and Request Contexts** using Lenses
    * **Static file-serving** capability with **Caching and Hot-Reload** 
    * **Single Page Application** support with **Caching and Hot-Reload** 
    * Servlet implementation to allow **plugin to any Servlet container**
    * Launch applications in **1LOC** with an embedded **SunHttp** server backend (recommended for development use only)
    * **Path-based websockets** including typesafe message marshalling using Lenses, which are **testable without a running container**
    * APIs to **record and replay** HTTP traffic to disk or memory
    * Core **abstraction APIs** implemented by the other modules 
* [Client:](https://http4k.org/guide/modules/clients) 
    * **1LOC** client adapters 
        * **Apache** sync + async HTTP
        * **Jetty** HTTP (supports sync and async HTTP)
        * **OkHttp** HTTP (supports sync and async HTTP)
        * **Java** (bundled with `http4k-core`)
    * **1LOC** WebSocket client, with blocking and non-blocking modes
* [Server:](https://http4k.org/guide/modules/servers)
    * **1LOC** server backend spinup for:
        * **Jetty** (including websocket support)
        * **Undertow**
        * **Apache** (from httpcore)
        * **Netty**
        * **Ktor CIO**
        * **SunHttp** (bundled with `http4k-core`)
    * API design allows for plugging into configurable instances of each
* [Serverless:](https://http4k.org/guide/modules/serverless)
    * AWS: Implement a single Factory method, then upload your [http4k] applications to AWS Lambda to be called from API Gateway. 
    * Google Cloud Functions: Implement a single Factory method, then upload your [http4k] applications to Google Cloud Functions with `GCloud`. 
    * Apache OpenWhisk: Implement a single Factory method, then upload your [http4k] applications to Google Cloud Functions with `GCloud`. 
* [Contracts:](https://http4k.org/guide/modules/contracts) 
    * Define **Typesafe** HTTP contracts, with required and optional path/query/header/bodies
    * **Typesafe** path matching
    * **Auto-validation** of incoming requests == **zero boilerplate validation code**
    * Self-documenting for all routes - eg. Built in support for live **OpenApi v2 and v3** description endpoints including **JSON Schema** model breakdown. 
* [Templating:](https://http4k.org/guide/modules/templating) 
    * **Pluggable** templating system support for:
        * **Dust** 
        * **Freemarker**
        * **Handlebars** 
        * **Pebble**
        * **Thymeleaf**
        * **Jade4j**
    * Caching and **Hot-Reload** template support
* **Message formats:** 
    * Consistent API provides first class support for marshalling formats to/from HTTP messages for:
        * **[JSON](https://www.http4k.org/guide/modules/json/)** - with support for: 
            * **Jackson** - includes support for **fully [automatic marshalling](https://http4k.org/guide/modules/json/#auto-marshalling-capabilities) of Data classes**
            * **Gson** - includes support for **fully [automatic marshalling](https://http4k.org/guide/modules/json/#auto-marshalling-capabilities) of Data classes**
            * **Moshi** - includes support for **fully [automatic marshalling](https://http4k.org/guide/modules/json/#auto-marshalling-capabilities) of Data classes**
            * **KotlinX Serialization** - official Kotlin JSON API. 
            * **Argo** - lightweight Java JSON API with zero dependencies.            
        * **[XML](https://www.http4k.org/guide/modules/xml/)** - includes support for:
            * **Jackson** - includes support for **fully automatic marshalling of Data classes**
            * **Xml** - includes support for **one way automatic marshalling of Data classes**
        * **[YAML](https://www.http4k.org/guide/modules/yaml/)** - includes support for:
            * **Jackson** - includes support for **fully automatic marshalling of Data classes**
* [Resilience:](https://http4k.org/guide/modules/resilience) 
    * Support for Circuits, Retrying, Rate-Limiting, Bulkheading via Resilience4J integration.
* [Metrics:](https://http4k.org/guide/modules/metrics) 
    * Support for plugging http4k apps into micrometer
* [Multipart:](https://http4k.org/guide/modules/multipart) 
    * Support for Multipart HTML forms, including Lens extensions for type-safe marshalling of fields.
* [AWS:](https://http4k.org/guide/modules/aws) 
    * Client filter to allow super-simple interaction with AWS services (via request signing)
* [OAuth Security](https://http4k.org/guide/modules/oauth) 
    * Implement OAuth Authorisation Code Grant flow with a single Interface
    * **Pre-configured** OAuth for following providers:
        * **Auth0** 
        * **Dropbox** 
        * **Google** 
        * **Soundcloud**
* [Cloud Native:](https://http4k.org/guide/modules/cloud_native)
    * Tooling to support operating [http4k] applications in orchestrated cloud environments such as Kubernetes and CloudFoundry. 12-factor configuration, dual-port servers and health checks such as liveness and readiness checking. 
* [WebDriver:](https://http4k.org/guide/modules/webdriver) 
    * Ultra-lightweight Selenium WebDriver implementation for [http4k] application.
* [Hamkrest:](https://http4k.org/guide/modules/hamkrest) 
    * A set of Hamkrest matchers for testing [http4k] Request and Response messages.
* [Kotest:](https://http4k.org/guide/modules/kotest) 
    * A set of Kotest matchers for testing [http4k] Request and Response messages.
* [Approval Testing:](https://http4k.org/guide/modules/approvaltests) 
    * JUnit 5 extensions for [Approval testing](http://approvaltests.com/) of [http4k] Request and Response messages.
* [Chaos:](https://http4k.org/guide/modules/chaos) 
    * API for declaring and injecting failure modes into [http4k] applications, allowing modelling and hence answering of "what if" style questions to help understand how code fares under failure conditions such as latency and dying processes.
* [Service Virtualisation:](https://http4k.org/guide/modules/servicevirtualisation) 
    * Record and replay versioned HTTP contracts to/from `Servirtium` Markdown format. Includes Servirtium MiTM server and simple JUnit extensions.
    
## Example [<img class="octocat" src="https://www.http4k.org/img/octocat-32.png"/>](https://github.com/http4k/http4k/blob/master/src/docs/cookbook/readme/example.kt)

This quick example is designed to convey the simplicity & features of [http4k] . See also the [quickstart](https://http4k.org/quickstart/) for the simplest possible starting point and demonstrates how to serve and consume HTTP services with dynamic routing.

To install, add these dependencies to your **Gradle** file:

```groovy
dependencies {
    implementation group: "org.http4k", name: "http4k-core", version: "3.260.0"
    implementation group: "org.http4k", name: "http4k-server-jetty", version: "3.260.0"
    implementation group: "org.http4k", name: "http4k-client-okhttp", version: "3.260.0"
}
```

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

fun main() {
    // we can bind HttpHandlers (which are just functions from  Request -> Response) to paths/methods to create a Route,
    // then combine many Routes together to make another HttpHandler
    val app: HttpHandler = routes(
        "/ping" bind GET to { _: Request -> Response(OK).body("pong!") },
        "/greet/{name}" bind GET to { req: Request ->
            val name: String? = req.path("name")
            Response(OK).body("hello ${name ?: "anon!"}")
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

## Acknowledgments

* [Dan Bodart](https://twitter.com/DanielBodart)'s [utterlyidle](https://github.com/bodar/utterlyidle)
* [Ivan Moore](https://twitter.com/ivanrmoore) for pairing on "BarelyMagical", a 50-line wrapper around utterlyidle to allow "Server as a Function"


<span class="github">

## Contributors

This project exists thanks to all the people who [contribute](https://www.http4k.org/contributing/).
<a href="https://github.com/http4k/http4k/graphs/contributors"><img src="https://opencollective.com/http4k/contributors.svg?width=890" /></a>

## Backers & Sponsors

If you use [http4k] in your project or enterprise and would like to support ongoing development, please consider becoming a backer or a sponsor. Sponsor logos will show up here with a link to your website.

<a href="https://opencollective.com/http4k/sponsor/2/website" target="_blank"><img src="https://opencollective.com/http4k/sponsor/2/avatar.svg"></a>
<a href="https://opencollective.com/http4k/sponsor/3/website" target="_blank"><img src="https://opencollective.com/http4k/sponsor/3/avatar.svg"></a>
<a href="https://opencollective.com/http4k/sponsor/4/website" target="_blank"><img src="https://opencollective.com/http4k/sponsor/4/avatar.svg"></a>
<a href="https://opencollective.com/http4k/sponsor/5/website" target="_blank"><img src="https://opencollective.com/http4k/sponsor/5/avatar.svg"></a>
<a href="https://opencollective.com/http4k/sponsor/6/website" target="_blank"><img src="https://opencollective.com/http4k/sponsor/6/avatar.svg"></a>
<a href="https://opencollective.com/http4k/sponsor/7/website" target="_blank"><img src="https://opencollective.com/http4k/sponsor/7/avatar.svg"></a>
<a href="https://opencollective.com/http4k/sponsor/8/website" target="_blank"><img src="https://opencollective.com/http4k/sponsor/8/avatar.svg"></a>
<a href="https://opencollective.com/http4k/sponsor/9/website" target="_blank"><img src="https://opencollective.com/http4k/sponsor/9/avatar.svg"></a>
<a href="https://opencollective.com/http4k/sponsor/0/website" target="_blank"><img src="https://opencollective.com/http4k/sponsor/0/avatar.svg"></a>

</span>

[http4k]: https://http4k.org
