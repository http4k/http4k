# reekwest

[![coverage](https://coveralls.io/repos/reekwest/reekwest/badge.svg?branch=master)](https://coveralls.io/github/reekwest/reekwest?branch=master)
[![kotlin](https://img.shields.io/badge/kotlin-1.1.2-blue.svg)](http://kotlinlang.org)
[![build status](https://travis-ci.org/reekwest/reekwest.svg?branch=master)](https://travis-ci.org/reekwest/reekwest)
[![bintray version](https://api.bintray.com/packages/reekwest/maven/reekwest/images/download.svg)](https://bintray.com/reekwest/maven/reekwest/_latestVersion)

A sensible implementation of HTTP for Kotlin

Features:
 * Immutable Request/Response
 * Same abstractions for client and server usage
 * Enables "HTTP application as a function"
 * Can be plugged to different libraries and containers. Current implementation includes:
   * Client: ApacheHttpClient
   * Server: Servlets, Jetty
 * No 3rd party dependency required to start

## Getting started

Here's how to create and use a basic HTTP handling function:

```kotlin
val app = { request: Request -> ok().bodyString("Hello, ${request.query("location")}!") }
val get = get("/").query("location", "John Doe")
val response = app(get)
assertThat(response.status, equalTo(OK))
assertThat(response.bodyString(), equalTo("Hello, John Doe!"))
```

## Using as a client

[![bintray version](https://api.bintray.com/packages/reekwest/maven/reekwest/images/download.svg)](https://bintray.com/reekwest/maven/reekwest-client-apache/_latestVersion)

```kotlin
val client = ApacheHttpClient()
val request = get("http://httpbin.org/get").query("location", "John Doe")
val response = client(request)
assertThat(response.status, equalTo(OK))
assertThat(response.bodyString(), containsSubstring("John Doe"))
```

## Using as a server

[![bintray version](https://api.bintray.com/packages/reekwest/maven/reekwest/images/download.svg)](https://bintray.com/reekwest/maven/reekwest-server-jetty/_latestVersion)

```kotlin
{ _: Request -> ok().bodyString("Hello World") }.startJettyServer()
```

That will make a server running on http://localhost:8000

### Routing

Reekwest comes with basic routing:

```kotlin
routes(
    GET to "/hello/{location:*}" by { request: Request -> ok().bodyString("Hello, ${request.path("location")}!") },
    POST to "/fail" by { request: Request -> Response(INTERNAL_SERVER_ERROR) }
).startJettyServer()
```
