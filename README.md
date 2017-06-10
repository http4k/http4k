<img src="http://http4k.org/img/logo_1100x200_blue_on_white.png"/>

**http4k** is an HTTP toolkit written in [Kotlin](https://kotlinlang.org/) that enables the serving and consuming of HTTP services in a functional and consistent way.

<span class="github">
<a href="https://travis-ci.org/http4k/http4k"><img alt="build status" src="https://travis-ci.org/http4k/http4k.svg?branch=master"></a>
<a href="https://coveralls.io/github/http4k/http4k?branch=master"><img alt="coverage" src="https://coveralls.io/repos/http4k/http4k/badge.svg?branch=master"></a>
<a href="http//www.apache.org/licenses/LICENSE-2.0"><img alt="GitHub license" src="https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat"></a>
<a href="https://bintray.com/http4k/maven/http4k-core/_latestVersion"><img alt="Download" src="https://api.bintray.com/packages/http4k/maven/http4k-core/images/download.svg"></a>
<a href="http://kotlinlang.org"><img alt="kotlin" src="https://img.shields.io/badge/kotlin-1.1.2-blue.svg"></a>
<a href="https://codebeat.co/projects/github-com-http4k-http4k-master"><img alt="codebeat badge" src="https://codebeat.co/badges/5b369ed4-af27-46f4-ad9c-a307d900617e"></a>
<a href="https://kotlin.link"><img alt="Awesome Kotlin Badge" src="https://kotlin.link/awesome-kotlin.svg"></a>
<a href="https://gitter.im/http4k/http4k"><img alt="Gitter" src="https://img.shields.io/badge/gitter-join%20chat-1dce73.svg"></a>
</span>

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

## Module feature overview
* [Core:](http://http4k.org/guide/modules/core) 
    * Base HTTP handler and **immutable HTTP message** objects, cookie handling. 
    * Commonly used HTTP functionalities provided as reusable Filters (caching, debugging, **Zipkin request tracing**)
    * **Path-based routing**, including nestable contexts
    * **Typesafe HTTP message construction/desconstruction** using Lenses
    * **Static file-serving** capability with **Caching and Hot-Reload** 
    * Servlet implementation to allow **zero-dependency plugin to any Servlet container**
    * Core abstraction APIs implemented by the other modules 
* [Client:](http://http4k.org/guide/modules/clients) 
    * **Single LOC** HTTP client adapters 
        * **Apache**
        * **OkHttp**
* [Server:](http://http4k.org/guide/modules/servers)
    * **Single LOC** server backend spinup for:
        * **Jetty**
        * **Netty**
        * **Undertow**
    * API design allows for plugging into configurable instances of each
* **BETA!** [Contracts:](http://http4k.org/guide/modules/contracts) 
   * Definite **Typesafe** HTTP contracts, defining required and optional path/query/header/bodies
   * **Typesafe** path matching
   * **Auto-validation** of incoming requests == **zero boilerplate validation code**
   * Self-documenting for all routes - eg. Built in support for live **Swagger** description endpoints including **JSON Schema** model breakdown. 
* [Templating:](http://http4k.org/guide/modules/templating) 
    * **Pluggable** templating system support for:
        * Handlebars 
    * Caching and **Hot-Reload** template support
* [Message formats:](http://http4k.org/guide/modules/message_formats) 
    * Consistent API provides first class support for marshalling JSON to/from HTTP messages for:
        * **Jackson** -includes support for **fully automatic marshalling of Data classes**)
        * **Gson** -includes support for **fully automatic marshalling of Data classes**)
        * **Argo**

## Acknowledgments

* [Dan Bodart](https://twitter.com/DanielBodart)'s [utterlyidle](https://github.com/bodar/utterlyidle)
* [Ivan Moore](https://twitter.com/ivanrmoore) for pairing on "BarelyMagical", a 50-line wrapper around utterlyidle to allow "Server as a Function"

