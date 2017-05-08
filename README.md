# http4k

[![coverage](https://coveralls.io/repos/http4k/http4k/badge.svg?branch=master)](https://coveralls.io/github/http4k/http4k?branch=master)
[![kotlin](https://img.shields.io/badge/kotlin-1.1.2-blue.svg)](http://kotlinlang.org)
[![build status](https://travis-ci.org/http4k/http4k.svg?branch=master)](https://travis-ci.org/http4k/http4k)
[![bintray version](https://api.bintray.com/packages/http4k/maven/http4k-core/images/download.svg)](https://bintray.com/http4k/maven/http4k-core/_latestVersion)

A sensible implementation of HTTP for Kotlin

Features:
 * Immutable Request/Response
 * Uses the same abstractions for client and server usage
 * Enables "HTTP application as a function" (i.e. one can test a whole app without an actual server)
 * Modular design so can be plugged to different libraries and containers. Current implementation includes:
   * Client: [ApacheHttpClient](#using-as-a-client) 
   * Server: [Jetty, Netty](#using-as-a-server)
   * JSON: [Argo, Jackson](#json)
   * Templating: [Handlebars](#templating)
 * No 3rd party dependency required to start

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


Core: ```compile group: "org.http4k", name: "http4k", version: "0.16.0"```

Apache Client: ```compile group: "org.http4k", name: "http4k-client-apache", version: "0.16.0"```

Contracts: ```compile group: "org.http4k", name: "http4k-contract", version: "0.16.0"```

Argo JSON: ```compile group: "org.http4k", name: "http4k-format-argo", version: "0.16.0"```

Jackson JSON: ```compile group: "org.http4k", name: "http4k-format-jackson", version: "0.16.0"```

Handlebars: ```compile group: "org.http4k", name: "http4k-template-handlebars", version: "0.16.0"'```

Jetty Server: ```compile group: "org.http4k", name: "http4k-server-jetty", version: "0.16.0"'```

Netty Server: ```compile group: "org.http4k", name: "http4k-server-netty", version: "0.16.0"'```
