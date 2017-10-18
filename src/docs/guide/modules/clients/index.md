### Installation (Gradle)
**Apache:** ```compile group: "org.http4k", name: "http4k-client-apache", version: "2.34.0"```

**OkHttp:** ```compile group: "org.http4k", name: "http4k-client-okhttp", version: "2.34.0"```

### About
Supported HTTP client adapter APIs are wrapped to provide an `HttpHandler` interface in 1 LOC.

Activate streaming mode by passing a `ResponseBodyMode` (default is non-streaming).

These examples are for the Apache HTTP client, but the API is similar for the others:

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/modules/clients/example.kt"></script>

Alternatively, all client adapter modules allow for custom configuration of the relevant Client configuration by
