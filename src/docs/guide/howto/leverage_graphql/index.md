title: http4k How-to: Leverage GraphQL
description: Recipe for using GraphQL plugins 

### Gradle setup

```kotlin
dependencies {
    implementation(platform("org.http4k:http4k-bom:4.41.4.0"))
    implementation("org.http4k:http4k-core")
    implementation("org.http4k:http4k-graphql")
}
```

### Code [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/howto/leverage_graphql/example.kt)

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/howto/leverage_graphql/example.kt"></script>
