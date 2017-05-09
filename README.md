# http4k

[![coverage](https://coveralls.io/repos/http4k/http4k/badge.svg?branch=master)](https://coveralls.io/github/http4k/http4k?branch=master)
[![kotlin](https://img.shields.io/badge/kotlin-1.1.2-blue.svg)](http://kotlinlang.org)
[![build status](https://travis-ci.org/http4k/http4k.svg?branch=master)](https://travis-ci.org/http4k/http4k)
[![bintray version](https://api.bintray.com/packages/http4k/maven/http4k-core/images/download.svg)](https://bintray.com/http4k/maven/http4k-core/_latestVersion)

http4k is an HTTP toolkit written in Kotlin that provides the ability to serve and consume HTTP services in a sensible and consistent way. 
It consists of a core library `http4k-core` providing a base HTTP implementation + a number of abstractions for various functionalities (such as 
servers, clients, templating etc) that are then provided in a set of optional add-on libraries.

The core axioms of the toolkit are:

* *Application as a Function:* Based on the famous [Twitter paper](https://monkey.org/~marius/funsrv.pdf), HTTP services can be composed of 2 types of simple function:
    * HttpHandler: `(Request) -> Response` - provides a remote call for processing a `Request`.
    * Filter: `(HttpHandler) -> HttpHandler` - adds pre or post processing to a `HttpHandler`. These filters are composed to make stacks of reusable behaviour that can then 
    be applied to a `HttpHandler`.
* *Immutablility:* All entities in the library are immutable unless their function explicitly disallows this.
* *Symmetric:* The `HttpHandler` interface is identical for both HTTP services and clients. This allows for simple offline testability of applications, as well as plugging together 
of services without HTTP container being required.
* *Dependency-lite:* The `http-core` module has ZERO dependencies. Add-on modules only have dependencies required for specific implementation.
* *Modularity:* Common behaviours are abstracted into the `http4k-core` module. Current add-ons cover:
   * Clients: [ApacheHttpClient](#using-as-a-client) 
   * Servers: [Jetty, Netty](#using-as-a-server)
   * Message formats: [Argo JSON, Jackson JSON](#json)
   * Templating: [Handlebars](#templating)

## Getting started

Here's how to create and use a basic HTTP handling function:

```kotlin
val app = { request: Request -> ok().body("Hello, ${request.query("name")}!") }
val get = get("/").query("name", "John Doe")
val response = app(get)
assertThat(response.status, equalTo(OK))
assertThat(response.bodyString(), equalTo("Hello, John Doe!"))
```

## Using as a client

```kotlin
val client = ApacheHttpClient()
val request = get("http://httpbin.org/get").query("location", "John Doe")
val response = client(request)
assertThat(response.status, equalTo(OK))
assertThat(response.bodyString(), containsSubstring("John Doe"))
```

## Using as a server

```kotlin
{ _: Request -> ok().body("Hello World") }.startJettyServer()
```

That will make a server running on http://localhost:8000

### Routing

Reekwest comes with basic routing:

```kotlin
routes(
    GET to "/hello/{name:*}" by { request: Request -> ok().body("Hello, ${request.path("name")}!") },
    POST to "/fail" by { request: Request -> Response(INTERNAL_SERVER_ERROR) }
).startJettyServer()
```

## Filters

Filters allow to add behaviour to existing handlers (or other Filters). 

For instance, to add basic authentication to a server:

```kotlin
val handler = { _: Request -> ok() }
val app = ServerFilters.BasicAuth("my realm", "user", "password").then(handler)
```

Similarly, to add basic authentitcation to a client:

```kotlin
val client = ClientFilters.BasicAuth("user", "password").then(ApacheHttClient())
```

## Other features

Creates `curl` command for a given request:

```kotlin
val curl = post("http://httpbin.org/post").body(listOf("foo" to "bar").toBody()).toCurl()
// curl -X POST --data "foo=bar" "http://httpbin.org/post"
```

## JSON

coming soon...

## Templating

coming soon...

## Installation

Add one or more of these module dependencies:


Core: ```compile group: "org.http4k", name: "http4k", version: "0.17.0"```

Apache Client: ```compile group: "org.http4k", name: "http4k-client-apache", version: "0.17.0"```

Contracts: ```compile group: "org.http4k", name: "http4k-contract", version: "0.17.0"```

Argo JSON: ```compile group: "org.http4k", name: "http4k-format-argo", version: "0.17.0"```

Jackson JSON: ```compile group: "org.http4k", name: "http4k-format-jackson", version: "0.17.0"```

Handlebars: ```compile group: "org.http4k", name: "http4k-template-handlebars", version: "0.17.0"'```

Jetty Server: ```compile group: "org.http4k", name: "http4k-server-jetty", version: "0.17.0"'```

Netty Server: ```compile group: "org.http4k", name: "http4k-server-netty", version: "0.17.0"'```
