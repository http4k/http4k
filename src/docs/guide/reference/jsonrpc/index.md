title: http4k JSON-RPC Module
description: Feature overview of the http4k-jsonrpc module

### Installation (Gradle)

```kotlin
dependencies {
    implementation(platform("org.http4k:http4k-bom:5.17.0.0"))
    implementation("org.http4k:http4k-jsonrpc")
}
```

### About

Support for JSON-RPC handlers, with support for both manual and automatic marshalling modes.

Each service method "name" is bound to a particular endpoint function and then the entire API is 
exposed as a standard http4k `HttpHandler`, so it can be composed with other HttpHandlers and Filters.

A specialised ErrorHandler can also be assigned to the RPC contract.

Note that in order to activate JSON RPC, you need to import one of the supported JSON modules.

#### Code [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/reference/jsonrpc/example.kt)

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/reference/jsonrpc/example.kt"></script>
