title: http4k HTTP description: An explanation of the core function types for dealing with HTTP applications

### HttpMessage

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
