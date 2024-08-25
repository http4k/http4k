title: http4k Azure Module
description: Feature overview of the http4k-azure module

### Installation (Gradle)

```kotlin
dependencies {
    implementation(platform("org.http4k:http4k-bom:5.29.0.0"))
    implementation("org.http4k:http4k-azure")
}
```

### About
This module provides a http4k compatible `HttpClient` so you can http4k-ise your use of the standard Azure SDKs libraries by plugging in a standard `HttpHandler`. This simplifies fault testing and means that you can print out the exact traffic which is going to Azure - which is brilliant for both debugging and writing Fakes. :)

#### Code [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/reference/aws/example_sdk.kt)

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/reference/azure/example_sdk.kt"></script>
