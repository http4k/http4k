title: http4k blog: Meet http4k
description: An overview of the Kotlin "Server as a Function" library, http4k

# Server as a Function. In Kotlin. Typesafe. Without the Server.

##### [@daviddenton][github]

## Meet [http4k]

[http4k] is an HTTP toolkit written in Kotlin that enables the serving and consuming 
of HTTP services in a functional and consistent way.

Whenever (yet another) new JVM HTTP framework is released, the inevitable question that rightly get asked is **"How it 
this different to X?"**. In this post, I'm going to briefly cover what [http4k] is, 
how we think it's different, and address some of those **bold claims** from the title of this post.

Here's a quick rundown of what we think those differences are:

* [http4k] is small. Written in pure, functional Kotlin, with zero dependencies.
* [http4k] is simple. Like, really simple. No static API magic, no annotations, 
no reflection.
* [http4k] is immutable. It relies on an immutable HTTP model, which makes it a 
snap to test and debug.
* [http4k] is symmetric. It supports remote calls as a first-class concern, and 
the remote HTTP model is identical to the incoming HTTP model.
* [http4k] is typesafe. Say goodbye to all your validation and marshalling boilerplate 
and hello to automatic request validation and data class-based contracts for HTTP bodies using the Lens API.
* [http4k] is serverless. Or rather - server independent. Test an app out of 
container and then deploy it into any supported local container with 1 LOC - or as a function into AWS Lambda.

### Oh god, not another framework! Why does this even exist?!?
Firstly - we don't consider [http4k] to be a framework - it's a set of libraries 
providing a functional toolkit to serve and consume HTTP services, focusing on simple, consistent, and testable APIs. 
Hence, whilst it does provide support for various APIs *relevant to serving and consuming HTTP*, it does not provide 
every integration under the sun - merely simple points to allow those integrations to be hooked in.

Another thing to say is that (not very much) of [http4k] is new - it's rather the 
distillation of 15 years worth of experience of using various server-side libraries and hence most of the good ideas are 
stolen. For instance - the routing module is inspired by [UtterlyIdle], the basic 
"Server as a function" model is stolen from [Finagle], and the contract module
 OpenApi/Swagger generator is ported from [Fintrospect]. 

With the growing adoption of Kotlin, we wanted something that would fully leverage the functional features of the language 
and it felt like a good time to start something from scratch, whilst avoiding the *magic* that plagues other frameworks. 
Hence, [http4k] is primarily designed to be a Kotlin-first library.

## Claim A: Small, simple, immutable.
Based on the awesome ["Your Server as a Function"] paper from Twitter, [http4k] 
apps are modelled by composing 2 types of simple, independent function. 

### Function 1: HttpHandler
An `HttpHandler` represents an HTTP endpoint. It's not even an Interface, modelled merely as a [Typealias]:
```kotlin
typealias HttpHandler = (Request) -> Response
```
Below is a entire [http4k] application that echoes the request body back in the response. It only relies on the `http4k-core` 
module, which itself has zero dependencies:
```kotlin
val app = { request: Request -> Response(OK).body(request.body) }
val server = app.asServer(SunHttp(8000)).start()
```
The `Request` and `Response` objects in there are immutable data classes/POKOs, so testing the app requires absolutely no 
extra infrastructure - just call the function, it's as easy as:
```kotlin
class AppTest {
    @Test
    fun `echoes request body`() {
        assertThat(app(Request(POST, "/").body("hello")), equalTo(Response(OK).body("hello")))
    }
}
```
To plug it into a different Server-backend, just depend on the relevant module (Jetty, Undertow, Netty, Apache (httpcore), 
Ktor CIO, Ktor Netty, and SunHttp are available) and change the call to `asServer()`.

### Function 2: Filter
`Filters` provides pre and post Request processing and are simply:
```kotlin
interface Filter : (HttpHandler) -> HttpHandler
```
For API conciseness and discoverability reasons this is modelled as an Interface and not a [Typealias] - it also has a 
couple of Kotlin [extension methods] to allow you to compose 
`Filters` with `HttpHandlers` and other `Filters`:
```kotlin
val setContentType = Filter { next ->
        { request -> next(request).header("Content-Type", "text/plain") }
    }
val repeatBody = Filter { next ->
        { request -> next(request.body(request.bodyString() + request.bodyString() }
    }
val composedFilter: Filter = repeatBody.then(setContentType)
val decoratedApp: HttpHandler = composedFilter.then(app)
```
Filters are also trivial to test independently, because they are generally just stateless functions.

### Routing
[http4k]'s nestable routing looks a lot like every other Sinatra-style framework these 
days, and allows for infinitely nesting `HttpHandlers` - this just exposes another `HttpHandler` so you can easily extract, 
test and reuse sets of routes as easily as you could with one:
```kotlin
val app: HttpHandler = routes(
    "/app" bind GET to decoratedApp,
    "/other" bind routes(
        "/delete" bind DELETE to { _: Request -> Response(OK) },
        "/post/{name}" bind POST to { request: Request -> Response(OK).body("you POSTed to ${request.path("name")}") }
    )
)
```

And that it - those functions are everything you need to know to write a simple [http4k] application. The `http4k-core` 
module rocks in at about 700kb, and has zero dependencies (other than the Kotlin language itself). Additionally, everything 
in the core is *functional and predictable* - there is no static API magic going on under the covers (making it difficult 
to have multiple apps in the same JVM), no annotations, no compiler-plugins, and no reflection.

## Claim B. Symmetric HTTP
Out of the multitude of JVM http frameworks out there, not many actually consider how you app talks to other services, 
yet in this Microservice™ world that's an absolutely massive part of what many apps do!

As per a core principle behind "Server as a Function", [http4k] provides a symmetric API for HTTP clients - ie. it's 
*exactly* the same API as is exposed in [http4k] server applications - the `HttpHandler`. Here's that entire API again, 
just in case you've forgotten:
```kotlin
typealias HttpHandler = (Request) -> Response
```
What does that mean in practice? Well - for one thing, it's less for your brain to learn  because you already know the API:
```kotlin
val client: HttpHandler = ApacheClient()
val response: Response = client(Request(GET, "http://server/path"))
```
For another, it means that since clients are *just function*s you can easily stub them for testing, and since applications 
and clients are interchangeable, they can be plugged together in memory without putting them on the network - which makes 
testing insanely fast:

```kotlin
fun MyApp1(): HttpHandler = { Response(OK) }
fun MyApp2(app1: HttpHandler): HttpHandler = { app1(it) }

val app1: HttpHandler = MyApp1()
val app2: HttpHandler = MyApp2(app1)
```
[http4k] provides a HTTP client adapters for both [Apache] and [OkHttp], 
all with streaming support.

## Claim C. Typesafe HTTP with Lenses
The immutable [http4k] model for HTTP objects contains all the usual suspect methods for getting values from the messages. 
For instance, if we are expecting a search parameter with a query containing a page number:
```kotlin
val request = Request(GET, "http://server/search?page=123")
val page: Int = request.query("page")!!.toInt
```
...but we also want to ensure that the expected values are both present and valid, since the above example will fail if 
either of those things is not true. For this purpose, we can use a [Lens] to enforce the expected HTTP contract.

The use of Lenses in [http4k] applications can remove the need for writing any parsing or validation code for all incoming 
data (including Forms), as validations are taken care of by the library. 

### Lens basics
A Lens is a bi-directional entity which can be used to either *get* or *set* a particular value from/onto an HTTP message. 
[http4k] provides a DSL to configure these lenses to target particular parts of the message, whilst at the same time 
specifying the requirement for those parts (i.e. mandatory or optional) and the type. For the above example, we could use 
the `Query` Lens builder and then `invoke()` the Lens on the message to extract the target value:
```kotlin
val pageLens = Query.int().required("page")
val page: Int = pageLens(Request(GET, "http://server/search?page=123"))
```
If the query parameter is missing or not an Int, the lens extraction operation will fail. There are similar Lens builder 
functions for all parts of the HTTP message (`Header`, `Path`, `Body`, `FormField` etc..), and functions for all common 
JVM primitive types. They are all completely typesafe - there is no reflection or magic going on - just marshalling of 
the various entities (in this case String to Int conversion).

In the case of failure, we need to apply a Filter to detect the errors and convert them to a BAD_REQUEST response:
```kotlin
val queryName = Query.string().required("name")
val app: HttpHandler = routes(
      "/post" bind POST to { request: Request -> Response(OK).body(queryName(request)) }
    )

val app = ServerFilters.CatchLensFailure.then(handler)(Request(GET, "/hello/2000-01-01?myCustomType=someValue"))
```

Lenses can also be applied with a correctly typed value (via `invoke()`) to set it *onto* a target object - and as HTTP 
messages in [http4k] are immutable, this results in a copy of the modified message:
```kotlin
val pageSizeLens = Header.int().required("page")
val page: Response = pageLens(Response(OK), 123)
// or apply multiple lenses using with()
val updated: Request = Request(GET, "/foo").with(pageLens of 123, pageSizeLens of 10)
```

Securely extracting JDK primitives from HTTP messages is great, but we really want to avoid primitives entirely and go 
straight to domain types. During construction of Lens, the builders allow mapping to occur so we can leverage Kotlin data 
classes. This works for both get and set operations:
```kotlin
data class MyDate(val value: LocalDate)
val dateQuery = Query.localDate().map(::MyDate, MyDate::value).required("date")
val myDate: MyDate = dateQuery(Request(GET, "http://server/search?date=2000-01-01"))
```

### Lensing HTTP bodies with Data classes
Some of the supported message libraries (eg. GSON, Jackson, Moshi, XML) provide the mechanism to automatically marshall 
data objects to/from JSON and XML using reflection (oops - looks like we broke our reflection promise - but technically 
we're not doing it ;) !). This behaviour is supported in [http4k] Lenses through the use of the `auto()` method, which 
will marshall objects to/from HTTP messages:

```kotlin
data class Email(val value: String)
data class Message(val subject: String, val from: Email, val to: Email)

val messageLens = Body.auto<Message>().toLens()
val body = """{"subject":"hello","from":{"value":"bob@git.com"},"to":{"value":"sue@git.com"}}"""
val message: Message = messageLens(Request(GET, "/").body(body))
```

This mechanism works for all incoming and outgoing JSON and XML Requests and Responses. To assist with developing whilst 
using this type of auto-marshalling, we have created a [tool](http://http4k-data-class-gen.herokuapp.com/) to automatically 
generate a set of data classes for a given messages.

## Claim D. Serverless
Ah yes - Serverless - the latest in the Cool Kids Club and killer fodder for the resume. Well, since [http4k] is server 
independent, it turns out to be fairly trivial to deploy full applications to [AWS Lambda], 
and then call them by setting up the [API Gateway] to proxy requests to the function. 
Effectively, the combination of these two services become just another Server back-end supported by the library.

In order to achieve this, only a single interface `AppLoader` needs to be implemented - this is responsible for creating 
the `HttpHandler` which is adapted to the API of the `ApiGatewayProxyRequest/ApiGatewayProxyResponse` used by AWS. As this 
is AWS, there is a fair amount of configuration required to make this possible, but the only [http4k] specific config is to:

1. Set the function execution to call `org.http4k.serverless.lambda.LambdaFunction::handle`
2. Set an environment variable for the Lambda `HTTP4K_BOOTSTRAP_CLASS` to the class of your `AppLoader` class.

Here's a simple example:
```kotlin
object TweetEcho : AppLoader {
    override fun invoke(env: Map<String, String>): HttpHandler = {
        Response(OK).body(it.bodyString().take(140))
    }
}
```
Since [http4k] is very dependency-light, full binary uploads of these AWS Lambdas tend to be very small - and by utilising 
[Proguard] we've seen the size of a Lambda UberJar go as small as 150kb.

Introduced in v3.0.0, this support is available in the `http4k-serverless-lambda` module.

## The final word(s)!
As pointed out above, `http4k-core` module has zero dependencies. It is also small, even though it also provides:

* Support for static file-serving with HotReload.
* A bunch of useful Filters for stuff like [Zipkin] Request Tracing.
* Support for Request Contexts.
* Facilities to record and replay HTTP traffic.

There are also a bunch of other modules available, all presented with the same concentration on Testability, API simplicity 
and consistency:
 
* ViewModel driven templating engine support (Handlerbars etc) with HotReload.
* Popular JSON/XML (Gson, Jackson, Moshi, etc) library support for HTTP bodies.
* Typesafe HTML Form and Multipart Forms processing, with support for Streaming uploads to a storage service. Forms can 
also be configured to collect errors instead of just rejecting outright.
* Typesafe contract module, providing live [OpenApi v2 & v3] documentation.
* [AWS] request signing.
* [Resilience4j] integration, including Circuit Breakers & Rate Limiting.
* Testing support via [Hamkrest] matchers and an in-memory 
[WebDriver] implementation.

Finally, [http4k] is **proven in production**, it has been adopted in at least 2 global investment banks and is serving 
the vast majority of traffic for a major publishing website (in the top 1000 sites globally according to 
[alexa.com] - ie. easily serving 10s of million hits per day on a few nodes) since March 2017. 

You can see a few example applications [here](/in_action/), including a bootstrap project for creating a 
[**Github -> Travis -> Heroku** CD pipeline](https://github.com/http4k/http4k-bootstrap) in a single command.

Well, that's it for this whirlwind tour - we hope you found it worth reading this far! We'd love you to try out [http4k] 
and feedback why you love/hate/are indifferent to it :) . And if you want to get involved or chat to the authors, we hang 
out in the friendly #http4k channel @ [slack.kotlinlang,org].

##### Footnotes
* **"But... but... but... asynchronous! And Webscale!"**, *I heard them froth*. Yes, you are correct - "Server as a Function" 
is based on asynchronous functions and [http4k] exposes a synchronous API. However, our experience suggests that for the 
vast majority of apps, this actually makes API integration harder unless you've got async all the way down - and that is 
assuming that async clients are actually available for all your various remote dependencies. We found that this plainly 
didn't matter for our use-cases so went for *Simple API™* instead... it's possible however that Kotlin co-routines will 
allow us to revisit this decision.
* **(UPDATE) Websockets?** Yep - simple, testable, and now available in v3.2.1! See the introductory [blog post] for details! 

[github]: http://github.com/daviddenton
[http4k]: https://http4k.org
["Your Server as a Function"]: https://monkey.org/~marius/funsrv.pdf
[Finagle]: https://twitter.github.io/finagle/
[Fintrospect]: http://fintrospect.io/
[UtterlyIdle]: https://github.com/bodar/utterlyidle
[Typealias]: https://kotlinlang.org/docs/reference/type-aliases.html
[extension methods]: https://kotlinlang.org/docs/reference/extensions.html
[blog post]: https://www.http4k.org/blog/typesafe_websockets/
[Lens]: https://www.schoolofhaskell.com/school/to-infinity-and-beyond/pick-of-the-week/basic-lensing
[slack.kotlinlang,org]: http://slack.kotlinlang.org/
[Apache]: https://hc.apache.org/
[OkHttp]: http://square.github.io/okhttp/
[Proguard]: https://www.guardsquare.com/en/proguard
[Zipkin]: http://zipkin.io/
[OpenApi/Swagger]: https://www.openapis.org/
[AWS]: https://aws.amazon.com/
[Resilience4j]: http://resilience4j.github.io/resilience4j/
[Hamkrest]: https://github.com/npryce/hamkrest
[WebDriver]: https://github.com/SeleniumHQ/selenium
[alexa.com]: https://alexa.com
[API Gateway]: https://aws.amazon.com/api-gateway
[AWS Lambda]: https://aws.amazon.com/lambda
