title: http4k How-to: Customise a Server backend
description: How to use write custom servers backends

### How to write a custom server implmentation

Whilst the http4k server modules ship with a sensibly configured standard server-backend setup, a lot of projects will require specialised implementations of the underlying server backend. http4k makes this easy with the `ServerConfig` interface.

### Gradle setup

```kotlin
dependencies {
    implementation(platform("org.http4k:http4k-bom:4.46.0.0"))
    implementation("org.http4k:http4k-core")
    implementation("org.http4k:http4k-server-jetty")
}
```

The example below shows a customised Jetty setup which enables HTTPS traffic by reimplementing the `ServerConfig` interface. The idea is that this single class will encapsulate the usage of the Server platform API behind the http4k abstraction and provide a simple way to reuse it across different applications.

### Code [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/howto/customise_a_server_backend/example.kt)

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/howto/customise_a_server_backend/example.kt"></script>
