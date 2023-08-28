title: http4k How-to: Routing API (Simple)
description: Recipes for using the http4k composable routing API

This example shows how to use the simple routing functionality to bind several routes.

For the typesafe contract-style routing, refer to [this](/guide/howto/integrate_with_openapi/) recipe instead,

### Gradle setup

```kotlin
dependencies {
    implementation(platform("org.http4k:http4k-bom:5.7.4.0"))
    implementation("org.http4k:http4k-core")
}
```

### Code [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/howto/simple_routing/example.kt)

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/howto/simple_routing/example.kt"></script>
