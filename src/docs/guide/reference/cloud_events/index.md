title: http4k Cloud Events support
description: Feature overview of the http4k-cloudevents module

### Installation (Gradle)

```kotlin
dependencies {
    implementation(platform("org.http4k:http4k-bom:4.41.1.0"))
    implementation("org.http4k:http4k-cloudevents")
}
```

The [Cloud Events](https://cloudevents.io/) spec defines a common format for Events produced by Cloud services.

http4k provides simple pluggability into the CloudEvents Java SDKs and custom event format libraries via the Lens system - making it trivial to both receive or send CloudEvents in the standard way.

### Example 

In this example we are using the Jackson JSONFormat which is included by default with the `http4k-cloudevents` module. If you want to also use the lenses to access typed EventData, you will also need this in your Gradle file:

```kotlin
dependencies {
    implementation(platform("org.http4k:http4k-bom:4.41.1.0"))
    // to access the lenses in the Jackson module
    implementation("org.http4k:http4k-format-jackson")
}
```

#### Code [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/reference/cloud_events/example.kt)

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/reference/cloud_events/example.kt"></script>

