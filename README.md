<div class="github">

<hr/>

<picture>
  <source 
    srcset="https://www.http4k.org/img/logo-readme-dark-mode.png" 
    media="(prefers-color-scheme: dark)">
  <img src="https://www.http4k.org/img/logo-readme.png" alt="http4k logo">
</picture>

<hr/>

<a href="https://github.com/http4k/http4k/actions/workflows/build.yaml"><img alt="build" src="https://github.com/http4k/http4k/actions/workflows/build.yaml/badge.svg"></a>
<a href="https://mvnrepository.com/artifact/org.http4k"><img alt="download" src="https://img.shields.io/maven-central/v/org.http4k/http4k-core"></a>
<a href="https://codecov.io/gh/http4k/http4k"><img src="https://codecov.io/gh/http4k/http4k/branch/master/graph/badge.svg" /></a>
<a href="http://www.apache.org/licenses/LICENSE-2.0"><img alt="GitHub license" src="https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat"></a>
<a href="https://codebeat.co/projects/github-com-http4k-http4k-master"><img alt="codebeat" src="https://codebeat.co/badges/5b369ed4-af27-46f4-ad9c-a307d900617e"></a>
<a href="https://kotlin.link"><img alt="awesome kotlin" src="https://kotlin.link/awesome-kotlin.svg"></a>
<a href="https://opencollective.com/http4k"><img alt="Kotlin Slack" src="https://img.shields.io/badge/chat-kotlin%20slack-orange.svg"></a>
<a href="https://opencollective.com/http4k"><img alt="back us!" src="https://opencollective.com/http4k/backers/badge.svg"></a>
<a href="https://opencollective.com/http4k"><img alt="sponsor us!" src="https://opencollective.com/http4k/sponsors/badge.svg"></a>

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

## Sounds cool! Where can I find out more?

You can find out all about the project on the [http4k] site.

## Quickstart

Bored with reading already and just want to get coding? Read the [quickstart](https://www.http4k.org/quickstart/) or take a look at
the [examples repo](https://github.com/http4k/examples), which
showcases a variety of [http4k] use-cases and features.

## Module feature overview

If you're needed it to build an HTTP application, there's an excellent chance that http4k has a module for it. Check out the reference guide sections for details on the over 65 different built in integrations.

## Acknowledgments

* [Dan Bodart](https://twitter.com/DanielBodart)'s **utterlyidle**
* Ivan Moore for pairing on the original hackday project - Barely Magical.
* You can see the amazing people and companies who have helped us to make http4k [here](https://http4k.org/contributing).

[http4k]: https://http4k.org 
