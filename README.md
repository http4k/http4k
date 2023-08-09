<div class="github">

<hr/>

<picture>
  <source 
    srcset="https://www.http4k.org/img/logo-readme-dark-mode.png" 
    media="(prefers-color-scheme: dark)">
  <img src="https://www.http4k.org/img/logo-readme.png" alt="http4k logo">
</picture>

<hr/>
    
<a href="https://github.com/http4k/http4k/actions/workflows/build.yaml"><img alt="build" src="https://github.com/http4k/http4k/actions/workflows/build.yaml/badge.svg"></a>
<a href="https://mvnrepository.com/artifact/org.http4k"><img alt="download" src="https://img.shields.io/maven-central/v/org.http4k/http4k-core"></a>
<a href="https://codecov.io/gh/http4k/http4k"><img src="https://codecov.io/gh/http4k/http4k/branch/master/graph/badge.svg" /></a>
<a href="http//www.apache.org/licenses/LICENSE-2.0"><img alt="GitHub license" src="https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat"></a>
<a href="https://codebeat.co/projects/github-com-http4k-http4k-master"><img alt="codebeat" src="https://codebeat.co/badges/5b369ed4-af27-46f4-ad9c-a307d900617e"></a>
<a href="https://kotlin.link"><img alt="awesome kotlin" src="https://kotlin.link/awesome-kotlin.svg"></a>
<a href="https://opencollective.com/http4k"><img alt="Kotlin Slack" src="https://img.shields.io/badge/chat-kotlin%20slack-orange.svg"></a>
<a href="https://opencollective.com/http4k"><img alt="back us!" src="https://opencollective.com/http4k/backers/badge.svg"></a>
<a href="https://opencollective.com/http4k"><img alt="sponsor us!" src="https://opencollective.com/http4k/sponsors/badge.svg"></a>

<hr/>

</div>

 [http4k] is a lightweight but fully-featured HTTP toolkit written in pure [Kotlin](https://kotlinlang.org/) that enables the serving and consuming of HTTP services in a functional and consistent way. [http4k] applications are *just* Kotlin functions. For example, here's a simple echo server:
 
```kotlin
 val app: HttpHandler = { request: Request -> Response(OK).body(request.body) }
 val server = app.asServer(SunHttp(8000)).start()
```

[http4k] consists of a lightweight core library, `http4k-core`, providing a base HTTP implementation and Server/Client implementations based on the JDK classes. Further servers, clients, serverless, templating, websockets capabilities are then implemented in add-on modules. [http4k] apps can be simply mounted into a running Server, Serverless platform, or compiled to GraalVM and run as a super-lightweight binary.

The principles of [http4k] are:

* **Application as a Function:** Based on the Twitter paper ["Your Server as a Function"](https://monkey.org/~marius/funsrv.pdf), all HTTP services can be composed 
of 2 types of simple function:
    * *HttpHandler:* `(Request) -> Response` - provides a remote call for processing a Request. 
    * *Filter:* `(HttpHandler) -> HttpHandler` - adds Request/Response pre/post processing. These filters are composed to make stacks of reusable behaviour that can 
    then be applied to an `HttpHandler`.
* **Immutability:** All entities in the library are immutable unless their function explicitly disallows this.
* **Symmetric:** The `HttpHandler` interface is identical for both HTTP services and clients. This allows for simple offline testability of applications, as well as 
plugging together of services without HTTP container being required.
* **Dependency-lite:** Apart the from Kotlin StdLib, `http4k-core` module has **ZERO** dependencies and weighs in at ~1mb. Add-on modules only have dependencies required for specific implementation.
* **Testability** Built by **TDD** enthusiasts, so supports **super-easy** mechanisms for both in-memory and port-based testing of:
    * individual endpoints
    * applications
    * websockets/sse
    * full suites of microservices
* **Portable** Apps are completely portable across deployment platform in either a Server-based, Serverless or Native binaries.
    
## Quickstart
Bored with reading already and just want to get coding? For the impatient, visit the [http4k toolbox](https://toolbox.http4k.org) to generate a complete project from the wide variety of http4k modules.

Alternatively, read the [quickstart](https://www.http4k.org/quickstart/) or take a look at the [examples repo](https://github.com/http4k/examples), which showcases a variety of [http4k] use-cases and features.

## Module feature overview
* [Core:](https://http4k.org/guide/reference/core) 
    * Base HTTP handler and **immutable HTTP message** objects, cookie handling. 
    * Commonly used HTTP functionalities provided as reusable Filters (caching, debugging, **Zipkin request tracing**)
    * **Path-based routing**, including nestable contexts
    * **Typesafe HTTP message construction/deconstruction and Request Contexts** using Lenses
    * Servlet implementation to allow **plugin to any Servlet container**
    * Launch applications in **1LOC** with an embedded **SunHttp** server backend (recommended for development use only)
    * Lightweight **JavaHttpClient** implementation - perfect for Serverless contexts where binary size is a factor.
    * **Path-based WebSockets** including typesafe message marshalling using Lenses, which are **testable without a running container**
    * **Path-based Server-Sent Events** which are **testable without a running container**
    * APIs to **record and replay** HTTP traffic to disk or memory
    * **Static file-serving** capability with **Caching and Hot-Reload**
    * **Single Page Application** support with **Caching and Hot-Reload**
    * **WebJars** support in **1LOC**`
* [Client:](https://http4k.org/guide/reference/clients) 
    * **1LOC** client adapters 
        * **Apache** sync + async HTTP
        * **Java** (bundled with `http4k-core`)
        * **Fuel** HTTP (supports sync and async HTTP)
        * **Jetty** HTTP (supports sync and async HTTP and websockets)
        * **OkHttp** HTTP (supports sync and async HTTP)
    * **1LOC** Websocket client, with blocking and non-blocking modes
    * **GraphQL** client (bundled with GraphQL module)
* [Server:](https://http4k.org/guide/reference/servers)
    * **1LOC** server backend spin-up for:
        * **Apache v4 & v5** (from httpcore)
        * **Jetty & Jetty Loom** (including SSE and Websocket support)
        * **Helidon Nima (Loom)**
        * **Ktor CIO & Netty**
        * **Netty** (including Websocket support)
        * **SunHttp & SunHttpLoom** (bundled with `http4k-core`)
        * **Undertow** (including SSE and Websocket support)
        * **Java-WebSocket** (Websocket support only)
    * API design allows for simple customization of underying backend.
    * **Native Friendly** Several of the supported backends can be compiled with **GraalVM** and **Quarkus** with zero configuration.
* [Serverless:](https://http4k.org/guide/reference/serverless)
    * **Function-based support for both HTTP and Event-based applications** via adapters, using the simple and testable `HttpHandler` and `FnHandler` types.
    * **AWS Lambda** Extend custom adapters to serve HTTP apps from APIGateway or use react to AWS events (without using the heavyweight AWS serialisation).
    * **Custom AWS Lambda Runtime** Avoid the heavyweight AWS runtime, or simply compile your [http4k] app to GraalVM and get cold-starts in a few milliseconds! 
    * **Google Cloud Functions** Extend custom adapters to serve HTTP apps from Google Cloud Functions or use react to GCloud events. 
    * **Apache OpenWhisk** Extend custom adapters to serve HTTP apps or react to JSON events in IBM Cloud/OpenWhisk installations.
    * **Azure Functions** Extend custom adapters to serve HTTP apps from AzureCloud. 
    * **Alibaba Function Compute** Extend custom adapters to serve HTTP apps from Alibaba.
    * **Tencent Serverless Cloud Functions** Extend custom adapters to serve HTTP apps from SCF.
* [Contracts:](https://http4k.org/guide/reference/contracts) 
    * Define **Typesafe** HTTP contracts, with required and optional path/query/header/bodies
    * **Typesafe** path matching
    * **Auto-validation** of incoming requests == **zero boilerplate validation code**
    * Self-documenting for all routes - eg. Built in support for live **OpenApi v2 and v3** description endpoints including **JSON Schema** model breakdown.
    * [Redoc and Swagger UI](https://http4k.org/guide/howto/create_a_swagger_ui) for OpenApi descriptions
* [Templating:](https://http4k.org/guide/reference/templating) 
    * **Pluggable** templating system support for:
        * **Freemarker**
        * **Handlebars** 
        * **Pebble**
        * **Rocker**
        * **Thymeleaf**
        * **Jade4j**
    * Caching and **Hot-Reload** template support
* **Message formats:** 
    * Consistent API provides first class support for marshalling formats to/from HTTP messages for:
        * **[JSON](https://www.http4k.org/guide/reference/json/)** - with support for: 
            * **Jackson** - includes support for **fully [automatic marshalling](https://http4k.org/guide/reference/json/#auto-marshalling-capabilities) of Data classes**
            * **Gson** - includes support for **fully [automatic marshalling](https://http4k.org/guide/reference/json/#auto-marshalling-capabilities) of Data classes**
            * **Klaxon** - includes support for **fully [automatic marshalling](https://http4k.org/guide/reference/json/#auto-marshalling-capabilities) of Data classes**
            * **KondorJson** - includes support for **fully [automatic marshalling](https://http4k.org/guide/reference/json/#auto-marshalling-capabilities) of Data classes**
            * **Moshi** - includes support for **fully [automatic marshalling](https://http4k.org/guide/reference/json/#auto-marshalling-capabilities) of Data classes**
            * **KotlinX Serialization** - official Kotlin JSON API. 
            * **Argo** - lightweight Java JSON API with zero dependencies.            
        * **[XML](https://www.http4k.org/guide/reference/xml/)** - includes support for:
            * **Jackson** - includes support for **fully automatic marshalling of Data classes**
            * **Xml** - includes support for **one way automatic marshalling of Data classes**
        * **[YAML](https://www.http4k.org/guide/reference/yaml/)** - includes support for:
            * **Jackson** - includes support for **fully automatic marshalling of Data classes**
            * **Moshi** - includes support for **fully automatic marshalling of Data classes**
        * **CSV** - includes support for: 
            * **Jackson** - CSV format for Jackson
* [Resilience4J:](https://http4k.org/guide/reference/resilience4j) 
    * Circuits, Retrying, Rate-Limiting, Bulkheading via Resilience4J integration
* [Micrometer:](https://http4k.org/guide/reference/micrometer) 
    * Support for plugging http4k apps into Micrometer.
* [Cloud Events:](https://http4k.org/guide/reference/cloud_events) 
    * Consume and produce CloudEvents using typesafe lenses.
* [OpenTelemetry:](https://http4k.org/guide/reference/opentelemetry) 
    * Instrument http4k apps with OpenTelemetry tooling.
* [Multipart:](https://http4k.org/guide/reference/multipart) 
    * Support for Multipart HTML forms, including Lens extensions for type-safe marshalling of fields.
* [GraphQL:](https://http4k.org/guide/reference/graphql) 
    * Integration with GraphQL Java library to route and serve Graph-based apps. Plus conversion of any HttpHandler to be a GraphQL client.
* [AWS:](https://http4k.org/guide/reference/aws) 
    * Plug a standard `HttpHandler` into the AWS v2 SDKs. This massively simplifies testing and allows for sniffing of the exact traffic going to AWS - brilliant for debugging and building fakes.
    * Client filter to allow super-simple interaction with AWS services (via request signing)
* [OAuth Security:](https://http4k.org/guide/reference/oauth) 
    * Implement OAuth Authorisation Code Grant flow with a single Interface
    * **Pre-configured** OAuth for following providers:
        * **Auth0** 
        * **Discord**
        * **Dropbox** 
        * **Facebook** 
        * **GitLab** 
        * **Google** 
        * **Soundcloud**
* [Digest Security:](https://http4k.org/guide/reference/digest)
    * Implement the [Digest Authentication](https://datatracker.ietf.org/doc/html/rfc2617) flow for clients and servers
    * Supports the null and Auth QoPs
    * Supports Proxy Authentication
* [Cloud Native:](https://http4k.org/guide/reference/cloud_native)
    * Tooling to support operating [http4k] applications in orchestrated cloud environments such as Kubernetes and CloudFoundry. 12-factor configuration, dual-port servers and health checks such as liveness and readiness checking. 
* [Approval Testing:](https://http4k.org/guide/reference/approvaltests)
    * JUnit 5 extensions for [Approval testing](http://approvaltests.com/) of [http4k] Request and Response messages.
* [Chaos:](https://http4k.org/guide/reference/chaos)
    * API for declaring and injecting failure modes into [http4k] applications, allowing modelling and hence answering of "what if" style questions to help understand how code fares under failure conditions such as latency and dying processes.
* [Hamkrest:](https://http4k.org/guide/reference/hamkrest) 
    * A set of Hamkrest matchers for testing [http4k] Request and Response messages.
* [Kotest:](https://http4k.org/guide/reference/kotest)
    * A set of Kotest matchers for testing [http4k] Request and Response messages.
* [Service Virtualisation:](https://http4k.org/guide/reference/servicevirtualisation)
    * Record and replay versioned HTTP contracts to/from `Servirtium` Markdown format. Includes Servirtium MiTM server and simple JUnit extensions.
* [Strikt:](https://http4k.org/guide/reference/strikt) 
    * A set of Strikt matchers for testing [http4k] Request and Response messages.
* [TracerBullet:](https://http4k.org/guide/reference/tracerbuller)
    * Visually document your applications using the JUnit plugin.
* [WebDriver:](https://http4k.org/guide/reference/webdriver)
    * Ultra-lightweight Selenium WebDriver implementation for [http4k] application.
    
## Example [<img class="octocat" src="https://www.http4k.org/img/octocat-32.png"/>](https://github.com/http4k/http4k/blob/master/src/docs/howz/readme/example.kt)

This quick example is designed to convey the simplicity & features of [http4k] . See also the [quickstart](https://http4k.org/quickstart/) for the simplest possible starting point and demonstrates how to serve and consume HTTP services with dynamic routing.

To install, add these dependencies to your **Gradle** file:

```kotlin
dependencies {
    implementation(platform("org.http4k:http4k-bom:5.6.2.1"))
    implementation("org.http4k:http4k-core")
    implementation("org.http4k:http4k-server-jetty")
    implementation("org.http4k:http4k-client-okhttp")
}
```

```kotlin
package guide.howto.readme

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

<a href="https://opencollective.com/http4k/sponsor/0/website" target="_blank"><img src="https://opencollective.com/http4k/sponsor/0/avatar.svg"></a>
<a href="https://opencollective.com/http4k/sponsor/0/website" target="_blank"><img src="https://opencollective.com/http4k/sponsor/1/avatar.svg"></a>
<a href="https://opencollective.com/http4k/sponsor/2/website" target="_blank"><img src="https://opencollective.com/http4k/sponsor/2/avatar.svg"></a>
<a href="https://opencollective.com/http4k/sponsor/3/website" target="_blank"><img src="https://opencollective.com/http4k/sponsor/3/avatar.svg"></a>
<a href="https://opencollective.com/http4k/sponsor/4/website" target="_blank"><img src="https://opencollective.com/http4k/sponsor/4/avatar.svg"></a>
<a href="https://opencollective.com/http4k/sponsor/5/website" target="_blank"><img src="https://opencollective.com/http4k/sponsor/5/avatar.svg"></a>
<a href="https://opencollective.com/http4k/sponsor/6/website" target="_blank"><img src="https://opencollective.com/http4k/sponsor/6/avatar.svg"></a>
<a href="https://opencollective.com/http4k/sponsor/7/website" target="_blank"><img src="https://opencollective.com/http4k/sponsor/7/avatar.svg"></a>
<a href="https://opencollective.com/http4k/sponsor/8/website" target="_blank"><img src="https://opencollective.com/http4k/sponsor/8/avatar.svg"></a>
<a href="https://opencollective.com/http4k/sponsor/9/website" target="_blank"><img src="https://opencollective.com/http4k/sponsor/9/avatar.svg"></a>

</span>

[http4k]: https://http4k.org 
