title: http4k How-to: Authentication for HTTP services
description: Recipes for how to secure and authenticate HTTP services

### Gradle setup

```kotlin
dependencies {
    implementation(platform("org.http4k:http4k-bom:4.44.1.0"))
    implementation("org.http4k:http4k-core")

    // for OAuth examples
    implementation("org.http4k:http4k-security-oauth")
}
```

http4k provides a set of Filters for authenticating into other HTTP services. Usage of these filters is shown below to authenticate into a service. Each authentication type is generally available using both dynamic and static credential provision and checking mechanisms.

### Code [<img class="octocat" src="/img/octocat-32.png"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/howto/secure_and_auth_http/example.kt)

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/howto/secure_and_auth_http/example.kt"></script>
