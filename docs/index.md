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
* **Dependency-lite:** The `http4k-core` module has ZERO dependencies. Add-on modules only have dependencies required for specific implementation.
* **Testability** Built by **TDD** enthusiasts, so supports **super-easy** mechanisms for both In and Out of Container testing of:
    * individual endpoints
    * applications
    * full suites of microservices
* **Modularity:** Common behaviours are abstracted into the `http4k-core` module. Current add-ons cover:
   * Pluggable HTTP client adapters for Apache and OkHttp
   * Pluggable Server backends: Single LOC Server spinup for Jetty, Netty and Undertow
   * Typesafe, auto-validating, self-documenting (via Swagger) contracts for HTTP services
   * HTTP message adapters for Argo JSON, Gson JSON and Jackson JSON (includes auto-marshalling)
   * Templating support: Caching and Hot-Reload engine support for Handlebars

## Rationale
**http4k** is the distillation of 15 years worth of experience of using various server-side libraries and we've stolen good ideas from everywhere we can. For instance - 
the routing module is inspired by [UtterlyIdle](https://github.com/bodar/utterlyidle), the "Server as a function" and filter model is stolen from 
[Finagle](https://twitter.github.io/finagle/), and the contract module/Swagger generator is ported from [Fintrospect](http://fintrospect.io). With the growing 
adoption of Kotlin, we wanted something that would fully leverage the features of the language and it felt like a good time to start something from scratch.

For our purposes, we needed something that:
1. Starts/stops ultra quickly.
1. Easily testable outside of an HTTP container, and testing should require little to no custom infrastructure.
1. Provides typesafe HTTP message deconstruction/construction (in this case via Lenses).
1. Automatically deals with contract breaches (missing/invalid params etc) to remove boilerplate.
1. Absolutely no magic involved: No reflection. No annotations.
1. Minimal dependencies (`http4k-core` has zero).
1. Automatic generation of Swagger documentation (including JSON Schema models).
1. Has a symmetric server/client API (`HttpHandler` should just be `Request -> Response`).
1. Has immutable Request/Response objects.

All of these things summed together allow us to construct entire suites of services which can be tested either wired together without HTTP, or spun up in containers 
in 1 LOC. The symmetric HTTP API also allows filter chains (often called interceptors in other frameworks) to be constructed into reusable units/stacks for both 
server and client sides (eg. logging/metrics/caching...) since they can be composed together for later use. We can also easily create simple Fake servers for any 
HTTP contract, which means (in combination with CDC suites) we can end-to-end test micro-services in an outside-in way (using GOOS-style acceptance tests). This 
means that you are easily able to answer questions like "what happens if this HTTP dependency continually takes > 5 seconds to respond?" - which is a question you 
can't easily answer if you're faking out your dependencies inside the HTTP boundary.

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


## Installation
All **http4k** libraries are available on Maven Central and JCenter. Add the following to your Gradle file, substituting in the latest version displayed in the sidebar:
```
compile group: "org.http4k", name: "http4k-core", version: "X.X.X"
```

## Acknowledgments

* [Dan Bodart](https://twitter.com/DanielBodart)'s [utterlyidle](https://github.com/bodar/utterlyidle)
* [Ivan Moore](https://twitter.com/ivanrmoore) for pairing on "BarelyMagical", a 50-line wrapper around utterlyidle to allow "Server as a Function"

