<hr/>
<img src="https://http4k.org/images/logo-http4k.png" alt="http4k logo">
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

[http4k] is a lightweight but fully-featured HTTP toolkit written in pure [Kotlin](https://kotlinlang.org/) that enables the serving and consuming of HTTP
services in a functional and consistent way. [http4k] applications are *just* Kotlin functions. For example, here's a simple echo server:

```kotlin
val app: HttpHandler = { request: Request -> Response(OK).body(request.body) }
val server = app.asServer(SunHttp(8000)).start()
```

You can read about the rationale and ethos of http4k [here](https://http4k.org/rationale)

The [http4k] platform consists of the following main ecosystems, all released under a single version:

- [http4k-core](./core) consists of a lightweight core library providing a base HTTP implementation and Server/Client implementations based on the JDK classes. Further servers, clients, serverless, templating, websockets capabilities are then implemented in add-on modules. [http4k] apps can be simply mounted into a running Server, Serverless platform, or compiled to GraalVM and run as a super-lightweight binary.
- [http4k-connect](./connect) is a lightweight API Client toolkit which includes libraries for connecting to popular third-party cloud
  services and AI backends using [http4k](https://http4k.org) compatible APIs, along with Fake implementations for usage during local
  testing.

<h2>Sounds cool! Where can I find out more?</h2>

You can find out all about the project on the [http4k] site.

## Installation
```kotlin
dependencies {
    // install the platform...
    implementation(platform("org.http4k:http4k-bom:5.37.0.1"))

    // ...then choose any moduless but at least the core
    implementation("org.http4k:http4k-core")
}
```

## Acknowledgments

* [Dan Bodart](https://twitter.com/DanielBodart)'s **utterlyidle**
* Ivan Moore for pairing on the original hackday project - Barely Magical.
* You can see the amazing people and companies who have helped us to make http4k [here](https://http4k.org/community).

[http4k]: https://http4k.org 
