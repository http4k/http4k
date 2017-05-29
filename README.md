# http4k

[![coverage](https://coveralls.io/repos/http4k/http4k/badge.svg?branch=master)](https://coveralls.io/github/http4k/http4k?branch=master)
[![build status](https://travis-ci.org/http4k/http4k.svg?branch=master)](https://travis-ci.org/http4k/http4k)
[![Download](https://api.bintray.com/packages/http4k/maven/http4k-core/images/download.svg)](https://bintray.com/http4k/maven/http4k-core/_latestVersion)
[![GitHub license](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](http://www.apache.org/licenses/LICENSE-2.0)
[![kotlin](https://img.shields.io/badge/kotlin-1.1.2-blue.svg)](http://kotlinlang.org)
[![codebeat badge](https://codebeat.co/badges/5b369ed4-af27-46f4-ad9c-a307d900617e)](https://codebeat.co/projects/github-com-http4k-http4k-master)
[![Gitter](https://img.shields.io/badge/gitter-join%20chat-1dce73.svg)](https://gitter.im/http4k/http4k)
[![Awesome Kotlin Badge](https://kotlin.link/awesome-kotlin.svg)](https://kotlin.link)

**http4k** is an HTTP toolkit written in [Kotlin](https://kotlinlang.org/) that enables the serving and consuming of HTTP services in a functional and consistent way.

It consists of a core library `http4k-core` providing a base HTTP implementation + a number of abstractions for various functionalities (such as 
servers, clients, templating etc) that are provided as optional add-on libraries.

The principles of the toolkit are:

* **Application as a Function:** Based on the Twitter paper ["Your Server as a Function"](https://monkey.org/~marius/funsrv.pdf), all HTTP services can be composed of 2 types of simple function:
    * *HttpHandler:* `(Request) -> Response` - provides a remote call for processing a Request. 
    * *Filter:* `(HttpHandler) -> HttpHandler` - adds Request/Response pre/post processing. These filters are composed to make stacks of reusable behaviour that can then 
    be applied to an `HttpHandler`.
* **Immutablility:** All entities in the library are immutable unless their function explicitly disallows this.
* **Symmetric:** The `HttpHandler` interface is identical for both HTTP services and clients. This allows for simple offline testability of applications, as well as plugging together 
of services without HTTP container being required.
* **Dependency-lite:** The `http4k-core` module has ZERO dependencies. Add-on modules only have dependencies required for specific implementation.
* **Modularity:** Common behaviours are abstracted into the `http4k-core` module. Current add-ons cover:
   * [Client:](#user-content-http-client-adapter-modules) HTTP client adapters for Apache and OkHttp
   * [Server:](#user-content-server-backend-modules) Single LOC Server spinup for Jetty, Netty and Undertow
   * [Contracts:](#user-content-contracts-module) Typesafe, auto-validating, self-documenting (via Swagger) HTTP services
   * [Message formats:](#user-content-message-format-modules) HTTP message adapters for Argo JSON, Gson JSON and Jackson JSON (includes auto-marshalling)
   * [Templating:](#user-content-templating-modules) Caching and Hot-Reload engine support for Handlebars

# Getting started
This simple example demonstates how to serve and consume HTTP services using **http4k**. 

To install, add these dependencies to your **Gradle** file:
```groovy
dependencies {
    compile group: "org.http4k", name: "http4k-core", version: "1.25.1"
    compile group: "org.http4k", name: "http4k-server-jetty", version: "1.25.1"
    compile group: "org.http4k", name: "http4k-client-apache", version: "1.25.1"
}
```

The following creates a simple endpoint, binds it to a Jetty server then starts, queries, and stops it.

```kotlin
import org.http4k.client.ApacheClient
import org.http4k.core.Request
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.server.asServer

fun main(args: Array<String>) {

    val app = { request: Request -> Response(OK).body("Hello, ${request.query("name")}!") }

    val jettyServer = app.asServer(Jetty(9000)).start()

    val request = Request(Method.GET, "http://localhost:9000").query("name", "John Doe")

    val client = ApacheClient()

    println(client(request))

    jettyServer.stop()
}
```

# See it in action:
* [Cookbook example code](https://github.com/http4k/http4k/tree/master/src/test/kotlin/cookbook)
* [Todo backend (simple version)](https://github.com/http4k/http4k-todo-backend)
* [Todo backend (typesafe contract version)](https://github.com/http4k/http4k-contract-todo-backend)
* [TDD'd example application](https://github.com/http4k/http4k-contract-example-app)
* [Stage-by-stage example of development process (London TDD style)](https://github.com/http4k/http4k/tree/master/src/test/kotlin/worked_example)

# Modules

## Core Module
**Gradle:** ```compile group: "org.http4k", name: "http4k-core", version: "1.25.1"```

The core module has ZERO dependencies and provides the following:
* Immutable versions of the HTTP spec objects (Request, Response, Cookies etc).
* HTTP handler and filter abstractions which models services as simple, composable functions.
* Simple routing implementation, plus `HttpHandlerServlet` to enable plugging into any Servlet engine. 
* [Lens](https://www21.in.tum.de/teaching/fp/SS15/papers/17.pdf) mechanism for typesafe destructuring and construction of HTTP messages.
* Abstractions for Servers, Clients, messasge formats, Templating etc.

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
* Request tracing headers (x-b3-traceid etc)
* Basic Auth
* Cache Control
* CORS
* Cookie handling
* Debugging request and responses

Check out the `org.http4k.filter` package for the exact list.

### Simple Routing
Basic routing for mapping a URL pattern to an `HttpHandler`:
```kotlin
routes(
    GET to "/hello/{name:*}" by { request: Request -> Response(OK).body("Hello, ${request.path("name")}!") },
    POST to "/fail" by { request: Request -> Response(INTERNAL_SERVER_ERROR) }
).asServer(Jetty(8000)).start()
```

Note that the `http4k-contract` module contains a more typesafe implementation of routing functionality.

### Typesafe parameter destructuring/construction of HTTP messages with Lenses
Getting values from HTTP messages is one thing, but we want to ensure that those values are both present and valid. 
For this purpose, we can use a [Lens](https://www21.in.tum.de/teaching/fp/SS15/papers/17.pdf). A Lens is a bi-directional 
entity which can be used to either get or set a particular value from/onto an HTTP message. **http4k** provides a DSL 
to configure these lenses to target particular parts of the message, whilst at the same time specifying the requirement 
for those parts (i.e. mandatory or optional). Some examples of declarations are:

```kotlin
val pathLocalDate = Path.localDate().of("date")
val requiredQuery = Query.required("myQueryName")
val nonEmptyQuery = Query.nonEmptyString().required("myNonEmptyQuery")
val optionalHeader = Header.int().optional("Content-Length")
val responseBody = Body.string(PLAIN_TEXT).toLens()
```

Most of the useful common JDK types are covered. However, if we want to use our own types, we can just use `map()`
```kotlin
data class CustomType(val value: String)
val requiredCustomQuery = Query.map(::CustomType, { it.value }).required("myCustomType")
```

To use the Lens, simply `invoke() or extract()` it using an HTTP message to extract the value, or alternatively `invoke() or inject()` it with the value if we are modifying (via copy) the message:

```kotlin
val handler = routes(
    GET to "/hello/{date:*}" by { request: Request -> 
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

ServerFilters.CatchLensFailure.then(handler(Request(Method.GET, "/hello/2000-01-01?myCustomType=someValue")))
```
With the addition of the `CatchLensFailure` filter, no other validation is required when using Lenses, as **http4k** will handle invalid requests by returning a BAD_REQUEST (400) response.

More convieniently for construction of HTTP messages, multiple lenses can be used at once to modify a message, which is useful for properly building both requests and responses in a typesafe way without resorting to string values (especially in URLs which should never be constructed using String concatenation):
```kotlin
val modifiedRequest: Request = Request(Method.GET, "http://google.com/{pathLocalDate}").with(
    pathLocalDate of LocalDate.now(),
    requiredQuery of "myAmazingString",
    optionalHeader of 123
)
```

### Other features
Creates `curl` command for a given request:

```kotlin
val curl = post("http://httpbin.org/post").body(listOf("foo" to "bar").toBody()).toCurl()
// curl -X POST --data "foo=bar" "http://httpbin.org/post"
```

## Server-backend Modules
**Gradle (Jetty):** ```compile group: "org.http4k", name: "http4k-server-jetty", version: "1.25.1"```

**Gradle (Netty):** ```compile group: "org.http4k", name: "http4k-server-netty", version: "1.25.1"```

**Gradle (Undertow):** ```compile group: "org.http4k", name: "http4k-server-undertow", version: "1.25.1"```

Server-backend modules provide a consistent API mount HttpHandlers into the specified container in 1 LOC, by simply passing a `ServerConfig` implementation (in this case `Jetty`):

```kotlin
{ _: Request -> Response(OK).body("Hello World") }.asServer(Jetty(8000)).start().block()
```
Alteratively, all server-backend modules allow for plugging **http4k** handlers into the relevant server API, which allows for custom Server configuration.

## HTTP Client Adapter Modules
**Gradle (Apache):** ```compile group: "org.http4k", name: "http4k-client-apache", version: "1.25.1"```
**Gradle (OkHttp):** ```compile group: "org.http4k", name: "http4k-client-okhttp", version: "1.25.1"```

Supported HTTP client adapter APIs are wrapped to provide an `HttpHandler` interface in 1 LOC:

```kotlin
val client = ApacheClient()
val request = Request(Method.GET, "http://httpbin.org/get").query("location", "John Doe")
val response = client(request)
println(response.status)
println(response.bodyString())
```

Alteratively, all client adapter modules allow for custom configuration of the relevant Client configuration.

## Contracts Module
**Gradle:** ```compile group: "org.http4k", name: "http4k-contract", version: "1.25.1"```

The `http4k-contract` module adds a much more sophisticated routing mechanism to that available in `http4k-core`. It adds the facility 
to declare server-side `Routes` in a completely typesafe way, leveraging the Lens functionality from the core. These `Routes` are 
combined into `RouteModules`, which have the following features:
* **Auto-validating** - the `Route` contract is automatically validated on each call for required-fields and type conversions, removing the requirement 
for any validation code to be written by the API user. Invalid calls result in a `HTTP 400 (BAD_REQUEST)` response. 
* **Self-describing:** - a generated endpoint is provided which describes all of the `Routes`  in that module. Implementations 
include [Swagger/OpenAPI](http://swagger.io/) documentation, including generation of [JSON schema](http://json-schema.org/) models for messages.
* **Security:** to secure the `Routes`  against unauthorised access. Current implementations include `ApiKey`.

#### 1. Defining a Route
Firstly, create a route with the desired contract of headers, queries and body parameters. 
```kotlin
val ageQuery = Query.int().required("age")
val body = Body.string(TEXT_PLAIN).toLens()

val route = Route("echo").taking(ageQuery).body(body)
```

#### 2. Dynamic binding of calls to an HttpHandler
Next, define a dynamic path for this `Route` and then bind it to a function which creates an `HttpHandler` for each invocation,
which receives the dynamic path elements from the path:
```kotlin
val ageQuery = Query.int().required("age")
val body = Body.string(TEXT_PLAIN).toLens()

fun echo(nameFromPath: String): HttpHandler = {
    request: Request ->
        val age = ageQuery(request)
        val sentMessage = body(request)
        Response(OK).with(
            body to "hello $nameFromPath you are $age. You sent $sentMessage"
        )
}

val serverRoute: ServerRoute = route.at(GET) / "echo" / Path.of("name") bind ::echo
```

#### 3. Combining Routes into Modules
Finally, `ServerRoutes` are added into a reusable `RouteModule` (several of which can be combined) and then this is turned into a standard `HttpHandler`.
```kotlin
val handler: HttpHandler = RouteModule(Root / "context", Swagger(ApiInfo("My great API", "v1.0"), Argo))
    .securedBy(ApiKey(Query.int().required("api"), { it == 42 }))
    .withRoute(serverRoute)
    .toHttpHandler()
```

When launched, Swagger format documentation (including JSON schema models) can be found at the route of the module.

For a more extended example, see the following example apps: 
* [Todo backend (typesafe contract version)](https://github.com/http4k/http4k-contract-todo-backend)
* [TDD'd example application](https://github.com/http4k/http4k-contract-example-app)


## Message Format Modules
**Gradle (Argo):**  ```compile group: "org.http4k", name: "http4k-format-argo", version: "1.25.1"```

**Gradle (Gson):**  ```compile group: "org.http4k", name: "http4k-format-gson", version: "1.25.1"```

**Gradle (Jackson):** ```compile group: "org.http4k", name: "http4k-format-jackson", version: "1.25.1"```

These modules add the ability to use JSON as a first-class citizen when reading from and to HTTP messages. Each implementation adds a set of 
standard methods and extension methods for converting common types into native JSON objects, including custom Lens methods for each library so that 
JSON node objects can be written and read directly from HTTP messages:

#### Extension method API:
```kotlin
val json = Jackson

val objectUsingExtensionFunctions =
    listOf(
        "thisIsAString" to "stringValue".asJsonValue(),
        "thisIsANumber" to 12345.asJsonValue(),
        "thisIsAList" to listOf(true.asJsonValue()).asJsonArray()
    ).asJsonObject()

println(objectUsingExtensionFunctions.asPrettyJsonString())
```

#### Direct JSON library API:
```kotlin
val objectUsingDirectApi = json.obj(
    "thisIsAString" to json.string("stringValue"),
    "thisIsANumber" to json.number(12345),
    "thisIsAList" to json.array(listOf(json.boolean(true)))
)

println(
    Response(OK).with(
        Body.json().toLens() to json.array(objectUsingDirectApi, objectUsingExtensionFunctions)
    )
)
```

## Templating Modules
**Gradle:** ```compile group: "org.http4k", name: "http4k-template-handlebars", version: "1.25.1"```

The pluggable **http4k** templating API adds `ViewModel` rendering for common templating libraries. The implementations provide the a number of renderers for views:
* Cached on the classpath
* Cached from the filesystem
* Hot-Reloading from the filesystem

```kotlin
data class Person(val name: String, val age: Int) : ViewModel

val renderer = HandlebarsTemplates().HotReload("src/test/resources")

val app: HttpHandler = {
    val viewModel = Person("Bob", 45)
    val renderedView = renderer(viewModel)
    Response(OK).body(renderedView)
}

println(app(Request(Method.GET, "/someUrl")))
```

## Application Testing
The creators of **http4k** takes testing very seriously - so seriously that there really isn't that much to say here! 
The API has been designed to make it as simple as possible to test both individual endpoints and entire applications in a consistent fashion, which is aided by remembering that:

1. The input and output `Request/Response` objects are immutable.
2. `HttpHandler` endpoints are just functions.
3. An entire **http4k** application is *just* an `HttpHandler`.

Because of the above, there really isn't much required in the way of "testing infrastructure" - no magic containers or test fixtures that you might find in other frameworks. 
Testing is just matter of calling the correct function! Additionally, because the server and client HttpHandler interfaces are symmetrical - moving between in and out of container contexts 
(or indeed even to another HTTP framework entirely) is just a matter of switching out the HttpHandler implementation from the constructed app (out of container) to an HTTP client (in-container).

That said, possibly the most useful thing is to demonstrate the process that we have developed to test micro-services. A simple example of the development process can be found 
[here](https://github.com/http4k/http4k/tree/master/src/test/kotlin/worked_example).

## Acknowledgments

* [Dan Bodart](https://twitter.com/DanielBodart)'s [utterlyidle](https://github.com/bodar/utterlyidle)
* [Ivan Moore](https://twitter.com/ivanrmoore) for pairing on "BarelyMagical", a 50-line wrapper around utterlyidle to allow "Server as a Function"

