title: http4k Configuration tooling
description: Feature overview of the http4k-config module

### Installation (Gradle)

```kotlin
dependencies {
    implementation(platform("org.http4k:http4k-bom:5.31.0.0"))
    implementation("org.http4k:http4k-config")
}
```

http4k applications are naturally at home operating in distributed, Cloud Native environments. Whilst simple to create, this module
provides requisite tooling to get apps up and running with the minimum of effort to enable the following operational aspects:

#### Quick start

Because http4k does not use reflection or annotation process for application startup, all of the supported Server-backends
start up and shutdown very quickly - this is crucial for cloud-based environments where an orchestration framework might move
instances around to redistribute load or avoid problematic server/rack/DCs.

#### Configuration

All application configuration should be injected via environmental variables. http4k provides an `Environment` object, along with
typesafe variable binding using the in-built Lenses mechanism. This typesafe API is consistent with the other usages of Lenses

#### Code [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/reference/config/example.kt)

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/reference/config/example.kt"></script>

