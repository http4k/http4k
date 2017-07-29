### Installation (Gradle)
```compile group: "org.http4k", name: "http4k-core", version: "2.16.0"```

### About
The core module has ZERO dependencies and provides the following:

- Immutable versions of the HTTP spec objects (Request, Response, Cookies etc).
- HTTP handler and filter abstractions which models services as simple, composable functions.
- Simple routing implementation, plus `HttpHandlerServlet` to enable plugging into any Servlet engine. 
- [Lens](https://www21.in.tum.de/teaching/fp/SS15/papers/17.pdf) mechanism for typesafe destructuring and construction of HTTP messages.
- Abstractions for Servers, Clients, JSON Message formats, Templating etc.
- `SunHttp` Single-LOC development Server-backend 

#### HttpHandlers 
In **http4k**, an HTTP service is just a typealias of a simple function:
```kotlin
typealias HttpHandler = (Request) -> Response
```

First described in this Twitter paper ["Your Server as a Function"](https://monkey.org/~marius/funsrv.pdf), this abstraction allows us lots of 
flexibility in a language like Kotlin, since the conceptual barrier to service construction is reduced to effectively nil. Here is the simplest example - note that we don't need any special infrastructure to create an `HttpHandler`, neither do we 
need to launch a real HTTP container to exercise it:
```kotlin
val handler = { request: Request -> Response(OK).body("Hello, ${request.query("name")}!") }
val get = Request(Method.GET, "/").query("name", "John Doe")
val response = app(get)

println(response.status)
println(response.bodyString())
```

To mount the `HttpHandler` in a container, the can simply be converted to a Servlet by calling ```handler.asServlet()```

### Filters
Filters add extra processing to either the Request or Response. In **http4k**, they are modelled as:
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
            val response = next(it)
            val latency = System.currentTimeMillis() - start
            println("I took $latency ms")
            response
    }
}
val latencyAndBasicAuth: HttpHandler = ServerFilters.BasicAuth("my realm", "user", "password").then(myFilter)
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

### Simple Routing
Basic routing for mapping a URL pattern to an `HttpHandler`:
```kotlin
routes(
    "/hello/{name:*}" to GET bind { request: Request -> Response(OK).body("Hello, ${request.path("name")}!") },
    "/fail" to POST bind { request: Request -> Response(INTERNAL_SERVER_ERROR) }
).asServer(Jetty(8000)).start()
```

Note that the `http4k-contract` module contains a more typesafe implementation of routing functionality.

### Typesafe parameter destructuring/construction of HTTP messages with Lenses
Getting values from HTTP messages is one thing, but we want to ensure that those values are both present and valid. 
For this purpose, we can use a [Lens](https://www21.in.tum.de/teaching/fp/SS15/papers/17.pdf). 

A Lens is a bi-directional entity which can be used to either **get** or **set** a particular value from/onto an HTTP message. **http4k** provides a DSL 
to configure these lenses to target particular parts of the message, whilst at the same time specifying the requirement for those parts (i.e. mandatory or optional). 

To utilise a lens, first you have to declare it with the form `<Location>.<configuration and mapping operations>.<terminator>`.

There is one "location" type for each part of the message, each with config/mapping operations which are specific to that location:

| Location  | Starting type | Applicable to           | Multiplicity         | Requirement terminator | Examples  |
------------|---------------|-------------------------|----------------------|------------------------|------------
| Query     | `String`      | `Request`               | Singular or multiple | Optional or Required   | `Query.optional("name")`<br/>`Query.required("name")`<br/>`Query.int().required("name")`<br/>`Query.localDate().multi.required("name")`<br/>`Query.map(::CustomType, { it.value }).required("name")` |
| Header    | `String`      | `Request` or `Response` | Singular or multiple | Optional or Required   | `Header.optional("name")`<br/>`Header.required("name")`<br/>`Header.int().required("name")`<br/>`Header.localDate().multi.required("name")`<br/>`Header.map(::CustomType, { it.value }).required("name")`|
| Path      | `String`      | `Request`               | Singular | Required   |  `Path.of("name")`<br/>`Path.int().of("name")`<br/>`Path.map(::CustomType, { it.value }).of("name")`|
| FormField | `String`      | `WebForm`               | Singular or multiple | Optional or Required   | `FormField.optional("name")`<br/>`FormField.required("name")`<br/>`FormField.int().required("name")`<br/>`FormField.localDate().multi.required("name")`<br/>`FormField.map(::CustomType, { it.value }).required("name")`|
| Body      | `ByteBuffer`  | `Request` or `Response` | Singular | Required   |  `Body.string(ContentType.TEXT_PLAIN).toLens()`<br/>`Body.json().toLens()`<br/>`Body.webForm(FormValidator.Strict, FormField.required("name")).toLens()` |

Once the lens is declared, you can use it on a target object to either get or set the value:

- Retrieving a value: use `<lens>.extract(<target>)`, or the more concise invoke form: `<lens>(<target>)`
- Setting a value: use `<lens>.inject(<target>)`, or the more concise invoke form: `<lens>(<value>, <target>)`

```kotlin
val pathLocalDate = Path.localDate().of("date")
val requiredQuery = Query.required("myQueryName")
val nonEmptyQuery = Query.nonEmptyString().required("myNonEmptyQuery")
val optionalHeader = Header.int().optional("Content-Length")
val responseBody = Body.string(PLAIN_TEXT).toLens()

// Most of the useful common JDK types are covered. However, if we want to use our own types, we can just use `map()`
data class CustomType(val value: String)
val requiredCustomQuery = Query.map(::CustomType, { it.value }).required("myCustomType")

//To use the Lens, simply `invoke() or extract()` it using an HTTP message to extract the value, or alternatively `invoke() or inject()` it with the value if we are modifying (via copy) the message:
val handler = routes(
    "/hello/{date:*}" to GET bind { request: Request -> 
         val pathDate: LocalDate = pathLocalDate(request) 
         // SAME AS: 
         // val pathDate: LocalDate = pathLocalDate.extract(request)
         
         val customType: CustomType = requiredCustomQuery(request)
         val anIntHeader: Int? = optionalHeader(request)

         val baseResponse = Response(OK)
         val responseWithHeader = optionalHeader(anIntHeader, baseResponse)
         // SAME AS:
         // val responseWithHeader = optionalHeader.inject(anIntHeader, baseResponse)
         
         responseBody("you sent $pathDate and $customType", responseWithHeader) 
      }
)

val app = ServerFilters.CatchLensFailure.then(handler(Request(Method.GET, "/hello/2000-01-01?myCustomType=someValue")))
//With the addition of the `CatchLensFailure` filter, no other validation is required when using Lenses, as **http4k** will handle invalid requests by returning a BAD_REQUEST (400) response.

//More convieniently for construction of HTTP messages, multiple lenses can be used at once to modify a message, which is useful for properly building both requests and responses in a typesafe way without resorting to string values (especially in 
URLs which should never be constructed using String concatenation):

val modifiedRequest: Request = Request(Method.GET, "http://google.com/{pathLocalDate}").with(
    pathLocalDate of LocalDate.now(),
    requiredQuery of "myAmazingString",
    optionalHeader of 123
)
```

### Other features
Creates `curl` command for a given request:

```kotlin
val curl = Request(POST, "http://httpbin.org/post").body(listOf("foo" to "bar").toBody()).toCurl()
// curl -X POST --data "foo=bar" "http://httpbin.org/post"
```
