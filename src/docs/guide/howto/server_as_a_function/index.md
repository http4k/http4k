title: http4k How-to: Server-as-a-Function
description: The simplest example of an http4k application 

This example is the simplest possible "server" implementation. Note that we are not spinning up a server-backend here - but the entire application(!) is testable by firing HTTP requests at it as if it were.

### Gradle setup

```kotlin
dependencies {
    implementation(platform("org.http4k:http4k-bom:5.2.1.0"))
    implementation("org.http4k:http4k-core")
}
```

### Code [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/howto/server_as_a_function/example.kt)

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/howto/server_as_a_function/example.kt"></script>
