# {{WORK IN PROGRESS}}

# Server as a Function. In Kotlin. Typesafe. Without the Server.

##### [@daviddenton](http://github.com/daviddenton) / november 2017

##Meet [**http4k**](https://http4k.org).

Whenever (yet another) new JVM HTTP framework is released, the inevitable question that rightly get asked is **"How it this different to X?"**. In this post, I'm going to briefly cover what [**http4k**](https://http4k.org) is, how it's different, and address some of those **bold claims** from the title.

Here's a quick rundown of what we think those differences are:

* [**http4k**](https://http4k.org) is small. Written in pure Kotlin, with zero dependencies.
* [**http4k**](https://http4k.org) is simple. Like, really simple. No magic, no annotations, no reflection.
* [**http4k**](https://http4k.org) is immutable. It relies on an immutable HTTP model, which makes it a snap to test and debug.
* [**http4k**](https://http4k.org) is symmetric. The remote HTTP model is the same as the incoming model.
* [**http4k**](https://http4k.org) is typesafe. Say goodbye to boilerplate and hello to auto-generated documentation.
* [**http4k**](https://http4k.org) is serverless. Or rather - server independent. Test an app locally and then deploy it into AWS Lambda with no changes.

The first thing to say is that (not very much) of [**http4k**](https://http4k.org) is new - rather the distillation of 15 years worth of experience of using various server-side libraries and we've stolen good ideas from everywhere we can. For instance - the routing module is inspired by [UtterlyIdle](https://github.com/bodar/utterlyidle), the basic "Server as a function" model is stolen from [Finagle](https://twitter.github.io/finagle/), and the contract module OpenApi/Swagger generator is ported from [Fintrospect](http://fintrospect.io/). With the growing adoption of Kotlin, we wanted something that would fully leverage the features of the language and it felt like a good time to start something from scratch.

### 1. Small, simple, immutable.
Based on the awesome ["Your Server as a Function"](https://monkey.org/~marius/funsrv.pdf) paper from Twitter, [**http4k**](https://http4k.org) apps are modelled by composing 2 types of function. The first is called `HttpHandler` and represents an HTTP endpoint. It's not even an interface, modelled merely as a [typealias](https://kotlinlang.org/docs/reference/type-aliases.html):
```kotlin
typealias HttpHandler = (Request) -> Response
```
Here's a entire [**http4k**](https://http4k.org) application, which echoes request body back a the user. It only relies on the `http4k-core` module, which itself has zero dependencies:
```kotlin
val app = { request: Request -> Response(OK).body(request.body) }
val server = app.asServer(SunHttp(8000)).start()
```
The `Request` and `Response` objects in there are immutable data classes/POKOs, so testing the app requires absolutely no extra infrastructure, and is as easy as:
```kotlin
class AppTest {
    @Test
    fun `responds as expected`() {
        assertThat(app(Request(POST, "/").body("hello")), equalTo(Response(OK).body("hello")))
    }
}
```
To plug it into a different Server-backend, just depend on the relevant module (Jetty, Undertow, Netty) and change the call to `asServer()`.

The second type of function that we use is a `Filter`, which provides pre and post Request processing:
```kotlin
interface Filter : (HttpHandler) -> HttpHandler
```
For discoverability reasons this is modelled as an interface and not a typealias - it also has a couple of Kotlin extension methods to allow you to compose `Filters` with `HttpHandlers` and other `Filters`:
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
[http4k](https://http4k.org)'s nestable routing looks a lot like every other Sinatra-style framework these days - and you can infinitely nest `HttpHandlers` - this just exposes another `HttpHandler`, so you can extract and reuse:
```kotlin
val app: HttpHandler = routes(
    "/app" bind GET to decoratedApp,
    "/{name}" bind POST to { request -> Response(OK).body("you POSTed to ${request.path("name")}") }
)
```

The `[http4k](https://http4k.org)-core` module contains a bunch of useful Filters, rocks in at about <1000 lines of code, and has zero dependencies (other than the Kotlin language itself). It's proven in production, driving traffic for a major publishing website (easily serving 10's of million hits per day on a few nodes) since March 2017.

### 2. Symmetric
Out of the multitude of JVM http frameworks out there, not many actually consider how you app talks to other services, yet in this Microservice™ world that's an absolutely massive part of what many apps do!

As per a core principle behind "Server as a Function", [**http4k**](https://http4k.org) provides a symmetric API for HTTP clients - ie. it's *exactly* the same API as is exposed in [**http4k**](https://http4k.org) server applications - the `HttpHandler`. Here's that entire API again, just in case you've forgotten:
```kotlin
typealias HttpHandler = (Request) -> Response
```
What does that mean in practice? Well - for one thing, it's less for your brain to think about because you already know the API:
```kotlin
val client: HttpHandler = ApacheClient()
val response: Response = client(Request(GET, "http://server/path"))
```

For another, it means that if applications and clients are interchangeable, you can plug them together in memory without putting them on the network - which makes testing insanely fast:

```kotlin
fun MyApp1(): HttpHandler = { Response(OK) }
fun MyApp2(app1: HttpHandler): HttpHandler = { app1(it) }

val app1: HttpHandler = MyApp1()
val app2: HttpHandler = MyApp2(app1)
```
### 3. Typesafe
{{tumbleweed}}

### 4. Serverless
{{tumbleweed}}

##### Footnotes
*"But... but... but... asynchronous! And Webscale!"*, I heard them froth. Yes, you are correct - "Server as a Function" is based on asynchronous functions and [**http4k**](https://http4k.org) is synchronous. However, we tried this already and found that for 99% of apps it actually makes things harder unless you've got async all the way down. We found that this plainly didn't matter for our use-case so went for Simple™... maybe Kotlin co-routines will make this simpler - we'll see.

### TODO: more things here