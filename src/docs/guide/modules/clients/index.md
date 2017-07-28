### Installation (Gradle)
**Apache:** ```compile group: "org.http4k", name: "http4k-client-apache", version: "2.14.0"```

**OkHttp:** ```compile group: "org.http4k", name: "http4k-client-okhttp", version: "2.14.0"```

### About
Supported HTTP client adapter APIs are wrapped to provide an `HttpHandler` interface in 1 LOC:

```kotlin
val client = ApacheClient()
val request = Request(Method.GET, "http://httpbin.org/get").query("location", "John Doe")
val response = client(request)
println(response.status)
println(response.bodyString())
```

Alternatively, all client adapter modules allow for custom configuration of the relevant Client configuration.
