title: http4k blog: http4k v4
description: In preparation for the upcoming release of v4, the http4k team thought we'd do a bit of a retrospective about all the things that have gone in the >260 releases of v3

# http4k v4 unleashed

##### november 2020 / the http4k team

Well, at last it's here - after 3 years - [http4k] v4! Following on from the [retrospective](/blog/retrospective_v3) that we did on version 3, we've been polishing up, tidying up the edges, and pushing out a bunch of changes to make the project sparkle. Ready? Then let's dive into the good stuff that's been going on at [http4k] towers.

#### "Four digits good" - the new http4k versioning scheme
Ah yes - versioning - everyone's favourite topic. The reason that [http4k] v3 has been around so long is that we've somewhat been abusing the [Semantic versioning](https://semver.org/) system, something which we've been unhappy with. Here's how it should work:

```
For Version <A>.<B>.<C>

A = We broke something on purpose. (Breaking API change)
B = Profit. (Feature / Improvement)
C = We broke something by accident. (Bug)
```

Up until now, both breaking and non-breaking API changes on v3 have been done through the second (B) digit of the version - which doesn't allow API users to know if they are expecting a break. At the same time, we wanted to keep major (A) version changes for when there's a big "marketing" release.

To get around this, we are introducing a new versioning scheme based on 4 digits:
```
For Version <A>.<B>.<C>.<D>

A = There's something we'd like the world to know. (Major change / Marketing)
B = We broke something on purpose. (Breaking API change)
C = Profit. (Feature / Improvement)
D = We broke something by accident. (Bug)
```

As you can see, for our users we'll be concentrating on changes in numbers A (occasional) and C (standard).

#### "Platforms, Guv. Thousands of 'em!" (well, more than a few...)
When [http4k] v3 was released, we only supported 3 JVM Server backends and 1 Serverless platform. Since then, we've added a bunch, and are now up to a massive 18 standard deployment options for http4k apps:
```
10 JVM Backends - Apache 4/5, Jetty, Ktor CIO & Netty, Netty, Ratpack, SunHttp and Undertow
+ any Servlet container!
2 Native platforms - GraalVM, Quarkus
6 Serverless platforms - Alibaba, AWS Lamba, Azure, Google Cloud, OpenWhisk (IBM/Adobe/Nimbella/Cloudstation), Tencent
```

Switching between platforms is super easy - just plug the standard `HttpHandler` into the the relevant [http4k] module class with a single line of code, and then configure your Serverless platform to call the relevant function. Here's an example for Google Cloud:
```kotlin
class TestFunction : GoogleCloudFunction(
    { req: Request -> Response(OK).body("hello world!") }
)
```

The good news is that testing your Serverless functions locally is identical to testing any other [http4k] app - and as ever there's no magic involved - just test them entirely in-memory, or bind them to a standard backend Server.

#### http4k Toolbox
As documented in the [Toolbox announcement post](/blog/guns_for_show), we've been busy consolidating a bunch of handy tools for generating code to work with [http4k] projects, and we christened this the **http4k Toolbox** and it's avaiable both as a [website](https://toolbox.httpk.org) and a command-line utility from Brew and SDKMan. From [Project Generation](https://toolbox.httpk.org/project) to our own more sophisticated [OpenAPI3 Generator](https://toolbox.httpk.org/openapi), we hope that this will be the essential Swiss Army Knife in every **http4k** developer's pocket.

#### New Routing implementation

#### OpenTelemetry: Monitor all the things!
3. OpenTelemetry is now supported, bringing a standard way of instrumenting apps with distributed tracing (XRay, Jaeger, Zipkin) and metrics. Docs here: https://www.http4k.org/guide/modules/opentelemetry/

#### Library API changes
Like the neat little worker bunnies we are, we've taken the opportunity to clean up the source code. All previously Deprecated code has been removed

#### The http4k website

#### Examples Repo
4. The examples repo continues to grow - there are now examples for deploying http4k apps to *GraalVM* and *Quarkus*, and an 
https://github.com/http4k/examples

#### http4k Connect
5. http4k-connect is our newest project which we hope to eventually standardise patterns for building 3rd party adapters to various backend services, and for building your own Fakes (backed by data-stores such as InMemory and Redis). 

[http4k]: https://http4k.org
[Slack]: http://slack.kotlinlang.org/
