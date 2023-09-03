title: http4k How-to: Client as a function
description: Recipes for using http4k to consume HTTP services

This example demonstrates using http4k as a client, to consume HTTP services. A client is just another HttpHandler.

### Gradle setup

```kotlin
dependencies {
    implementation(platform("org.http4k:http4k-bom:5.7.5.0"))
    implementation("org.http4k:http4k-core")
}
```

### Code [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/howto/client_as_a_function/example.kt)

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/howto/client_as_a_function/example.kt"></script>
