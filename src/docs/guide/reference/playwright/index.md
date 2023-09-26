title: http4k Webdriver Module
description: Feature overview of the http4k-webdriver module

### Installation (Gradle)

```kotlin
dependencies {
    implementation(platform("org.http4k:http4k-bom:5.8.2.0"))
    implementation("org.http4k:http4k-testing-playwright")
}
```

### About

A JUnit extension for simply testing your http4k applications using the Playwright browser-automation library.

Create your application as normal and pass to the JUnit extension when registering it. The application is then launched
on a random port and a connected browser object injected into the test - requests to non-http URLs are automatically
routed to your app.

#### Code [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/reference/playwright/example.kt)

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/reference/playwright/example.kt"></script>

[http4k]: https://http4k.org
