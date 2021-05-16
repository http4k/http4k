title: http4k HTTP 
description: An explanation of the core function types for dealing with HTTP applications

HTTP application use-cases are the original and primary focus of **http4k**. Based on the [Server as a Function](https://monkey.org/~marius/funsrv.pdf) concept, http4k provides a set of function types which can be used to write, test and deploy HTTP applications with simplicity and ease.

Make no mistake - this model is deceptively simple but exceptionally powerful. These core concepts are repeated and combined in many different ways over the various functionalities provided by the toolkit.

### HttpMessage
In http4k, an HttpMessage is an immutable entity representing either a `Request` or a `Response`. 

This immutability is a powerful alternative to the mutable versions found in other web libraries, as it provides a exact record of the state of the messages as they travel through an HTTP application, so for debugging purposes you can time-travel through an application inspecting the exact state at any point in the stack. 

Data class semantics for comparison also make HttpMessages incredibly simple to assert against in testing scenarios, and this ease of testing is one of the most important parts of the [http4k ethos](/concepts/rationale).

### HttpHandler

```kotlin
typealias HttpHandler = (Request) -> Response 
```

A simple function representing all incoming and outgoing HTTP calls.

HttpHandlers can be bound to a container (to create an `Http4kServer`) with 1 LOC. The decouples the server
implementation from the business logic:

```kotlin
val jettyServer = app.asServer(Jetty(9000)).start()
```

An Http client is also a `HttpHandler`:

```kotlin
val client: HttpHandler = ApacheClient()
```

Because the client and server interfaces are the same, apps can simply be plugged together out-of-container by just
injecting one into the other:

```kotlin
val app1: HttpHandler = MyApp1()
val app2: HttpHandler = MyApp2(app1)
```

### Filter

```kotlin
fun interface Filter : (HttpHandler) -> HttpHandler
```

A function which decorates an HttpHandler to perform pre/post request processing. Filters can be composed together to
make reusable "stacks" of behaviour which can be applied to a terminating HttpHandler - to yield another, decorated,
HttpHandler.

### Router

```kotlin
interface Router {
    fun match(request: Request): RouterMatch
}
```

A selective request handler, which attempts to match an incoming call against a bound HttpHandler. 
