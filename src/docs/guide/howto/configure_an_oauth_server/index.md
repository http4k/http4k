title: http4k How-to: Configure an OAuth_Server
description: Recipe for using http4k to create an authorization server that provides an *authorization code* access flow

### Gradle setup

```kotlin
dependencies {
    implementation(platform("org.http4k:http4k-bom:5.6.3.0"))
    implementation("org.http4k:http4k-core")
    implementation("org.http4k:http4k-security-oauth")
}
```

For this example, you need to configure `OAuthServer` instance with the correct implementations of your login pages, generation of authentication codes and access tokens.

### Code [<img class="octocat" src="/img/octocat-32.png"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/howto/configure_an_oauth_server/example.kt)

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/howto/configure_an_oauth_server/example.kt"></script>
