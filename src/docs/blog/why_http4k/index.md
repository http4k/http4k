# {{WORK IN PROGRESS}}

# Server as a Function. In Kotlin. Typesafe. Without the Server.

##Meet http4k.

Whenever (yet another) new HTTP framework is released, the inevitable question that rightly get asked is "How it this different to X?". In this post, I'm going to briefly cover what http4k is, why it's different, and address some of those bold claims from the title.

Here's a quick rundown of what we think those differences are:
* http4k is small. Written in pure Kotlin, with zero dependencies.
* http4k is simple. Like, really simple. No magic, no annotations, no reflection.
* http4k is immutable. It relies on an immutable HTTP model, which makes it a snap to test and debug.
* http4k is symmetric. The remote HTTP model is the same as the incoming model.
* http4k is typesafe. Say goodbye to boilerplate and hello to auto-generated documentation.
* http4k is serverless. Or rather - server independent. Test an app locally and then deploy it into AWS Lambda with no changes.

The first thing to say is that (not very much) of http4k is new - rather the distillation of 15 years worth of experience of using various server-side libraries and we've stolen good ideas from everywhere we can. For instance - the routing module is inspired by UtterlyIdle, the basic "Server as a function" model is stolen from Finagle, and the contract module OpenApi/Swagger generator is ported from Fintrospect. With the growing adoption of Kotlin, we wanted something that would fully leverage the features of the language and it felt like a good time to start something from scratch.

### 1. Small, simple, immutable.
Based on the awesome "Your Server as a Function" paper from Twitter, http4k apps are modelled by comoosing 2 types of function. The first is called `HttpHandler`, and represents an HTTP endpoint. It's not even an interface, modelled merely as a typealias:
```kotlin
typealias HttpHandler = (Request) -> Response
```
Here's a entire http4k application, which echoes request body back a the user. It only relies on the `http4k-core` module, which itself has zero dependencies:
```kotlin
val app = { request: Request -> Response(OK).body(request.body) }
val server = app.asServer(SunHttp(8000)).start()
```
The `Request` and `Response` objects in there are immutable data classes, so testing the app requires absolutely no extra infrastructure, and is as easy as:
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
http4k's nestable routing looks a lot like every other Sinatra-style framework these days - and you can infinitely nest `HttpHandlers` - this just exposes another `HttpHandler`, so you can extract and reuse:
```kotlin
val app: HttpHandler = routes(
    "/app" bind GET to decoratedApp,
    "/{name}" bind POST to { request -> Response(OK).body("you POSTed to ${request.path("name")}") }
)
```

The `http4k-core` module contains a bunch of useful Filters, rocks in at about <1000 lines of code, and has zero dependencies (other than the Kotlin language itself). It's proven in production, driving traffic for a major publishing website (easily serving 10's of million hits per day on a few nodes) since March 2017.

### TODO: more things here