title: http4k HTTP
description: An explanation of the core function types for dealing with HTTP applications

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
