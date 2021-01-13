title: http4k Rationale & Concepts
description: Overview of why we created http4k, the problems it solves, and the core concepts

### tldr;
[Presentation](https://www.http4k.org/server_as_a_function_in_kotlin) about the development of http4k given at the Kotlin London meetup.

### About
**http4k** is the distillation of 15 years worth of experience of using various server-side libraries and we've stolen good ideas from everywhere we can. For instance - 
the routing module is inspired by [UtterlyIdle](https://github.com/bodar/utterlyidle), the "Server as a function" and filter model is stolen from 
[Finagle](https://twitter.github.io/finagle/), and the contract module OpenApi/Swagger generator is ported from [Fintrospect](http://fintrospect.io). With the growing 
adoption of Kotlin, we wanted something that would fully leverage the features of the language and it felt like a good time to start something from scratch.

For our purposes, we needed something that:

1. Starts/stops ultra quickly.
1. Easily testable outside of an HTTP container, and testing should require little to no custom infrastructure.
1. Provides typesafe HTTP message deconstruction/construction (in this case via Lenses).
1. Automatically deals with contract breaches (missing/invalid params etc) to remove boilerplate.
1. Absolutely no magic involved: No reflection. No annotations.
1. Minimal dependencies (apart from the Kotlin StdLib, `http4k-core` has zero).
1. Automatic generation of OpenApi/Swagger documentation (including JSON Schema models).
1. Has a symmetric server/client API (`HttpHandler` should just be `Request -> Response`).
1. Has immutable Request/Response objects.

**http4k** ticks all of these boxes. 

It allow us to construct entire suites of services which can be tested either wired together without HTTP, or spun up in containers using a single line of code. The symmetric HTTP API also allows Filter chains (often called "Middleware" or "Interceptors" in other frameworks) to be constructed into reusable units/stacks for both 
server and client sides (eg. logging/metrics/caching...) since they can be composed together for later use. 

As a bonus, we can also easily create simple Fake servers for any HTTP contract, which means (in combination with CDC suites) you can end-to-end test micro-services in an outside-in way (using GOOS-style acceptance tests).

Scenarios such as "what happens if this HTTP dependency continually takes > 5 seconds to respond?" are easily modelled - answers you can't easily get if you're faking out your dependencies inside the HTTP boundary.

### Concepts

* All incoming and outgoing HTTP services are modelled as `HttpHandler`, which is modelled as `(Request) -> Response`:
```kotlin
val handler: HttpHandler = { request: Request -> Response(OK) }
```
* Pre/post processing is done using a `Filter`, which is modelled as `(HttpHandler) -> HttpHandler`. Filters can therefore be composed together to make reusable "stacks" of behaviour which can be applied to a terminating `HttpHandler` - to yield 
a decorated `HttpHandler`:
```kotlin
    val filter: Filter = Filter { next: HttpHandler ->
        { request: Request -> next(request).header("my response header", "value") }
    }
    val decorated: HttpHandler = filter.then(handler)
```
* Binding an `HttpHandler` to a path and HTTP verb yields a `RoutingHttpHandler`, which is both an `HttpHandler` and a`Router`:
```kotlin
val route: RoutingHttpHandler = "/path" bind GET to { Response(OK).body("you GET bob") }
```
* `RoutingHttpHandler`s can be grouped together:
```kotlin
val app: RoutingHttpHandler = routes(
    "/bob" bind GET to { Response(OK).body("you GET bob") },
    "/rita" bind POST to { Response(OK).body("you POST rita") },
    "/sue" bind DELETE to { Response(OK).body("you DELETE sue") }
)
```
* A `Router` is a selective request handler, which attempts to match a request. If it cannot, processing falls through to the next `Router` in the list.
* `Routers` can be combined together (under particular context roots) to form another `RoutingHttpHandler`:
```kotlin
val bigApp: HttpHandler = routes(
    "/this" bind app, 
    "/other" bind app
)
```
* `HttpHandlers` can be bound to a container (to create an `Http4kServer`) with 1 LOC. The decouples the server implementation from the business logic:
```kotlin
val jettyServer = app.asServer(Jetty(9000)).start()
```
* An Http client is also a `HttpHandler`:
```kotlin
val client: HttpHandler = ApacheClient()
```
* Because the client and server interfaces are the same, apps can simply be plugged together out-of-container by just injecting one into the other:
```kotlin
    val app1: HttpHandler = MyApp1()
    val app2: HttpHandler = MyApp2(app1)
```
