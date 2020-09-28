title: http4k Core Module
description: Feature overview of the http4k-core module, including the Lens system for typesafe HTTP

### Installation (Gradle)

```groovy
implementation group: "org.http4k", name: "http4k-core", version: "3.261.0"
```

### About
Apart from Kotlin StdLib, the core module has ZERO dependencies and provides the following:

- Immutable versions of the HTTP spec objects (Request, Response, Cookies etc).
- HTTP handler and filter abstractions which models services as simple, composable functions.
- Simple routing implementation, plus `HttpHandlerServlet` to enable plugging into any Servlet engine. 
- [Lens](https://www.schoolofhaskell.com/school/to-infinity-and-beyond/pick-of-the-week/basic-lensing) mechanism for typesafe destructuring and construction of HTTP messages.
- Typesafe Request Context operations using Lenses.
- Abstractions for Servers, Clients, JSON Message formats, Templating, Websockets etc.
- `SunHttp` Ultra-fast single-LOC development server-backend
- Static file-serving capability with **Caching** and **Hot-Reload**
- APIs to **record and replay** HTTP traffic to disk or memory

#### HttpHandlers 
In [http4k], an HTTP service is just a typealias of a simple function:
```kotlin
typealias HttpHandler = (Request) -> Response
```

First described in this Twitter paper ["Your Server as a Function"](https://monkey.org/~marius/funsrv.pdf), this abstraction allows us lots of 
flexibility in a language like Kotlin, since the conceptual barrier to service construction is reduced to effectively nil. Here is the simplest example - note that we don't need any special infrastructure to create an `HttpHandler`, neither do we 
need to launch a real HTTP container to exercise it:
```kotlin
val handler = { request: Request -> Response(OK).body("Hello, ${request.query("name")}!") }
val get = Request(Method.GET, "/").query("name", "John Doe")
val response = handler(get)

println(response.status)
println(response.bodyString())
```

To mount the `HttpHandler` in a container, the can simply be converted to a Servlet by calling ```handler.asServlet()```

### Filters
Filters add extra processing to either the Request or Response. In [http4k], they are modelled as:
```kotlin
interface Filter : (HttpHandler) -> HttpHandler
``` 

Filters are designed to simply compose together (using `then()`) , creating reusable stacks of behaviour which can then be applied to any `HttpHandler`. 
For example, to add Basic Auth and latency reporting to a service:
```kotlin
val handler = { _: Request -> Response(OK) }

val myFilter = Filter {
    next: HttpHandler -> {
        request: Request -> 
            val start = System.currentTimeMillis()
            val response = next(request)
            val latency = System.currentTimeMillis() - start
            println("I took $latency ms")
            response
    }
}
val latencyAndBasicAuth: Filter = ServerFilters.BasicAuth("my realm", "user", "password").then(myFilter)
val app: HttpHandler = latencyAndBasicAuth.then(handler)
```

The `http4k-core` module comes with a set of handy Filters for application to both Server and Client `HttpHandlers`, covering common things like:

- Request tracing headers (x-b3-traceid etc)
- Basic Auth
- Cache Control
- CORS
- Cookie handling
- Debugging request and responses

Check out the `org.http4k.filter` package for the exact list.

### Routers - Nestable, path-based Routing
Create a Router using routes() to bind a static or dynamic path to either an HttpHandler, or to another sub-Router. These Routers can be nested infinitely deep and [http4k] will search for a matching route using a depth-first search algorithm, before falling back finally to a 404:
```kotlin
routes(
    "/hello" bind routes(
        "/{name:.*}" bind GET to { request: Request -> Response(OK).body("Hello, ${request.path("name")}!") }
    ),
    "/fail" bind POST to { request: Request -> Response(INTERNAL_SERVER_ERROR) }
).asServer(Jetty(8000)).start()
```

Note that the `http4k-contract` module contains a more typesafe implementation of routing functionality, with runtime-generated live documentation in OpenApi/Swagger format.

### Typesafe parameter destructuring/construction of HTTP messages with Lenses
Getting values from HTTP messages is one thing, but we want to ensure that those values are both present and valid. 
For this purpose, we can use a [Lens](https://www.schoolofhaskell.com/school/to-infinity-and-beyond/pick-of-the-week/basic-lensing). 

A Lens is a bi-directional entity which can be used to either **get** or **set** a particular value from/onto an HTTP message. [http4k] provides a DSL 
to configure these lenses to target particular parts of the message, whilst at the same time specifying the requirement for those parts (i.e. mandatory or optional). 

To utilise a lens, first you have to declare it with the form `<Location>.<configuration and mapping operations>.<terminator>`.

There is one "location" type for each part of the message, each with config/mapping operations which are specific to that location:

| Location  | Starting type | Applicable to           | Multiplicity         | Requirement terminator | Examples  |
------------|---------------|-------------------------|----------------------|------------------------|------------
| Query     | `String`      | `Request`               | Singular or multiple | Optional or Required   | `Query.optional("name")`<br/>`Query.required("name")`<br/>`Query.int().required("name")`<br/>`Query.localDate().multi.required("name")`<br/>`Query.map(::CustomType, { it.value }).required("name")` |
| Header    | `String`      | `Request` or `Response` | Singular or multiple | Optional or Required   | `Header.optional("name")`<br/>`Header.required("name")`<br/>`Header.int().required("name")`<br/>`Header.localDate().multi.required("name")`<br/>`Header.map(::CustomType, { it.value }).required("name")`|
| Path      | `String`      | `Request`               | Singular | Required   |  `Path.of("name")`<br/>`Path.int().of("name")`<br/>`Path.map(::CustomType, { it.value }).of("name")`|
| FormField | `String`      | `WebForm`               | Singular or multiple | Optional or Required   | `FormField.optional("name")`<br/>`FormField.required("name")`<br/>`FormField.int().required("name")`<br/>`FormField.localDate().multi.required("name")`<br/>`FormField.map(::CustomType, { it.value }).required("name")`|
| Body      | `ByteBuffer`  | `Request` or `Response` | Singular | Required   |  `Body.string(ContentType.TEXT_PLAIN).toLens()`<br/>`Body.json().toLens()`<br/>`Body.webForm(Validator.Strict, FormField.required("name")).toLens()` |

Once the lens is declared, you can use it on a target object to either get or set the value:

- Retrieving a value: use `<lens>.extract(<target>)`, or the more concise invoke form: `<lens>(<target>)`
- Setting a value: use `<lens>.inject(<value>, <target>)`, or the more concise invoke form: `<lens>(<value>, <target>)`

#### Code [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/modules/core/example.kt)

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/modules/core/example.kt"></script>

### Serving static assets
For serving static assets, just bind a path to a Static block as below, using either a Classpath or Directory (Hot reloading) based ResourceLoader instance (find these on the `ResourceLoader` companion object). Typically, Directory is used during development and the Classpath strategy is used to serve assets in production from an UberJar. This is usually based on a "devmode" flag when constructing your app". **Note** that you should avoid setting the Classpath value to the root because otherwise it will serve anything from your classpath (including Java class files!)!:
```kotlin
routes(
    "/static" bind static(Classpath("/org/http4k/some/package/name")),
    "/hotreload" bind static(Directory("path/to/static/dir/goes/here"))
)
```

### Single Page Apps
These can be easily activated as below, and default to serving from `/public` package:
```kotlin
routes(
    "/api" bind { Response(OK).body("some api content") },
    singlePageApp()
)
```

### Typesafe Websockets.
Websockets have been modeled using the same methodology as standard HTTP endpoints - ie. with both simplicity and testability as a first class concern, as well as benefiting from Lens-based typesafety. Websocket communication consists of 3 main concepts:

1. `WsHandler` - represented as a typealias: `WsHandler =  (Request) -> WsConsumer?`. This is responsible for matching an HTTP request to a websocket.
1. `WsConsumer` - represented as a typealias: `WsConsumer = (WebSocket) -> Unit`. This function is called on connection of a websocket and allow the API user to react to events coming from the connected websocket.
1. `WsMessage` - a message which is sent or received on a websocket. This message can take advantage of the typesafety accorded to other entities in http4k by using the Lens API. Just like the [http4k](https://github.com/http4k/http4k) HTTP message model, WsMessages are immutable data classes.

The routing aspect of Websockets is done using a very similar API to the standard HTTP routing for HTTP messages and dynamic parts of the upgrade request are available when constructing a websocket instance:

```kotlin
data class Wrapper(val value: String)

val body = WsMessage.string().map(::Wrapper, Wrapper::value).toLens()

val nameLens = Path.of("name")

val ws: WsHandler = websockets(
    "/hello" bind websockets(
        "/{name}" bind { ws: WebSocket ->
            val name = nameLens(ws.upgradeRequest)
            ws.send(WsMessage("hello $name"))
            ws.onMessage {
                val received = body(it)
                ws.send(body(received))
            }
            ws.onClose {
                println("closed")
            }
        }
    )
)
```

A `WsHandler` can be combined with an `HttpHandler` into a `PolyHandler` and then mounted into a supported backend server using `asServer()`:
```kotlin
val app = PolyHandler(
    routes(
        "/" bind { r: Request -> Response(OK) }
    ),
    websockets(
        "/ws" bind { ws: WebSocket -> ws.send(WsMessage("hello!"))
        }
    )
)
app.asServer(Jetty(9000)).start()
```
Alternatively, the `WsHandler` can be also converted to a synchronous `WsClient` - this allows testing to be done completely offline, which allows for super-fast tests:
```kotlin
val client = app.testWsClient(Request(Method.GET, "ws://localhost:9000/hello/bob"))!!

client.send(WsMessage("1"))
client.close(Status(200, "bob"))

client.received.take(2).forEach(::println)
```

### Request and Response toString()
The HttpMessages used by [http4k] toString in the HTTP wire format, which it simple to capture and replay HTTP message streams later in a similar way to tools like [Mountebank](http://www.mbtest.org/).

### CURL format
Creates `curl` command for a given request - this is useful to include in audit logs so exact requests can be replayed if required:

```kotlin
val curl = Request(POST, "http://httpbin.org/post").body(listOf("foo" to "bar").toBody()).toCurl()
// curl -X POST --data "foo=bar" "http://httpbin.org/post"
```

[http4k]: https://http4k.org
