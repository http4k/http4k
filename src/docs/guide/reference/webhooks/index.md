title: http4k Webhooks Module
description: Feature overview of the http4k-webhooks module

### Installation (Gradle)

```kotlin
dependencies {
    implementation(platform("org.http4k:http4k-bom:5.25.0.0"))
    implementation("org.http4k:http4k-webhooks")
}
```

### About

This module provides infrastructure for the [Webhook standard](https://www.standardwebhooks.com/), providing infrastructure for 
signing and verifying of Webhook requests (HMAC256 only currently) as per the standard, and support for the defined Webhook event wrapper format.

The example below shows how to use sign and verify filters to automatically provide security and marshalling for the Standard Webhook format.

#### Code [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/reference/webhooks/example.kt)

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/reference/webhooks/example.kt"></script>

[http4k]: https://http4k.org
