# reekwest

[![coverage](https://coveralls.io/repos/reekwest/reekwest/badge.svg?branch=master)](https://coveralls.io/github/reekwest/reekwest?branch=master)
[![kotlin](https://img.shields.io/badge/kotlin-1.1.2-blue.svg)](http://kotlinlang.org)
[![build status](https://travis-ci.org/reekwest/reekwest.svg?branch=master)](https://travis-ci.org/reekwest/reekwest)
[![bintray version](https://api.bintray.com/packages/reekwest/maven/reekwest/images/download.svg)](https://bintray.com/reekwest/maven/reekwest/_latestVersion)

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
val app = { request: Request -> ok().bodyString("Hello, ${request.query("name")}!") }
val get = get("/").query("name", "John Doe")
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

## Filters

Filters allow to add behaviour to existing handlers (or other Filters). 

For instance, to add basic authentication to a server:

```kotlin
val handler = { _: Request -> ok() }
val app = BasicAuthServer("my realm", "user", "password").then(handler)
```

Similarly, to add basic authentitcation to a client:

```kotlin
val client = BasicAuthClient("user", "password").then(ApacheHttClient())
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


Core: ```compile group: "org.reekwest", name: "reekwest", version: "0.0.32"```

Apache Client: ```compile group: "org.reekwest", name: "reekwest-client-apache", version: "0.0.32"```

Contracts: ```compile group: "org.reekwest", name: "reekwest-contract", version: "0.0.32"```

Argo JSON: ```compile group: "org.reekwest", name: "reekwest-formats-argo", version: "0.0.32"```

Jackson JSON: ```compile group: "org.reekwest", name: "reekwest-formats-jackson", version: "0.0.32"```

Handlebars: ```compile group: "org.reekwest", name: "reekwest-templates-handlebars", version: "0.0.32"'```

Jetty Server: ```compile group: "org.reekwest", name: "reekwest-server-jetty", version: "0.0.32"'```

Netty Server: ```compile group: "org.reekwest", name: "reekwest-server-netty", version: "0.0.32"'```
