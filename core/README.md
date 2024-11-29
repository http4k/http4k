<div class="github">
<hr/>
<img src="https://http4k.org/images/logo-http4k.png" alt="http4k logo">
<hr/>

</div>

[http4k] is a lightweight but fully-featured HTTP toolkit written in pure [Kotlin](https://kotlinlang.org/) that enables the serving and consuming of HTTP
services in a functional and consistent way. [http4k] applications are *just* Kotlin functions. For example, here's a simple echo server:

```kotlin
val app: HttpHandler = { request: Request -> Response(OK).body(request.body) }
val server = app.asServer(SunHttp(8000)).start()
```

[http4k] consists of a lightweight core library, `http4k-core`, providing a base HTTP implementation and Server/Client implementations based on the JDK classes.
Further servers, clients, serverless, templating, websockets capabilities are then implemented in add-on modules. [http4k] apps can be simply mounted into a
running Server, Serverless platform, or compiled to GraalVM and run as a super-lightweight binary.

You can read about the rationale and ethos of http4k [here](https://http4k.org/rationale)

<div class="github">
<h2>Sounds cool! Where can I find out more?</h2>

You can find out all about the project on the [http4k] site.
</div>

## Installation

```kotlin
dependencies {
    // install the platform...
    implementation(platform("org.http4k:http4k-bom:5.37.0.1"))

    // ...then choose any moduless but at least the core
    implementation("org.http4k:http4k-core")
}
```

## Quickstart

Bored with reading already and just want to get coding? Read the [quickstart](https://www.http4k.org/quickstart/) or take a look at
the [examples repo](https://github.com/http4k/examples), which
showcases a variety of [http4k] use-cases and features.

## Module feature overview

If you're needed it to build an HTTP application, there's an excellent chance that http4k has a module for it. Check out the reference guide sections for
details on the over 65 different built in integrations. Here's a selection:

- **Seamless app running in all these runtimes**:
    - **Server**: 8 server integrations including Jetty, Helidon, Undertow and Ktor
    - **Servlets**: Plug into any HTTP Servlet container
    - **Serverless**: Support for 6 platforms including AWS Lambda, GCP, Alibaba and Azure
    - **Custom Lambda runtime**: For running performance sensitive AWS Lambdas without the overhead of the AWS runtime
    - **Native**: http4k apps can compile to GraalVM with no changes or configuration
    - **In-memory**: Running apps in a JVM for lightning fast tests
    - **Realtime**: WebSockets and Server Sent Events (SSE)
- **OpenAPI**: Document your APIs with industry leading support
- **Protocol formats:** Support for lots of protocol formats including JSONRpc, Graphql
- **Pluggable wire format integrations for**: JSON, YAML, CSV, XML, DataFrame
- **Clients are available for these technologies**:
    - **HTTP**: 6 clients including Apache, Jetty and OkHttp
    - **WebSocket & SSE**: For realtime connectivity
- **Observability**: Measure http4l with integrations including OpenTelemetry and Micrometer
- **Metrics**: Protect your app with Resilience4k and Failsafe
- **Security**: Seamless OAuth and Digest integrations
- **HTML Templating**: 7 serverside rendering engines including Handlebars, Pug4J and Rocker
- **Testing styles**: Support for innovative testing techniques such as TracerBullet, Approval and Chaos testing
- **Testing Integrations**: Battle harden your apps with integrations for WebDriver, Playwright, KoTest and others

## Acknowledgments

* [Dan Bodart](https://twitter.com/DanielBodart)'s **utterlyidle**
* Ivan Moore for pairing on the original hackday project - Barely Magical.
* You can see the amazing people and companies who have helped us to make http4k [here](https://http4k.org/community).

[http4k]: https://http4k.org 
