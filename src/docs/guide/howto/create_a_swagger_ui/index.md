title: Swagger UI
description: Create a Swagger UI for your REST API

Http4k's contract module has a few built-in options to render a Swagger UI for your **OpenApi** v2 or v3 description.
For more detail on generating contracts, see:

- [Http4k Reference: Contracts](/guide/reference/contracts)
- [http4k How-to: Integrate with OpenAPI](/guide/howto/integrate_with_openapi)

## Thin UI

Rather than host the Swagger UI assets locally, a minimal wrapper will load them from a public CDN.
This is excellent for applications requiring a minimal footprint (such as serverless functions).
However, you have less control over the availability and performance of the distribution.

### Installation (Gradle)

```kotlin
dependencies {
    implementation(platform("org.http4k:http4k-bom:4.41.1.1"))
    implementation("org.http4k:http4k-contract")
}
```

### Example [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/howto/create_a_swagger_ui/example.kt)

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/howto/create_a_swagger_ui/example.kt"></script>

## Bundled UI

Http4k will bundle the Swagger UI distribution into your server.
This option can be more reliable, but will contribute to a larger jar size.

### Installation (Gradle)

```kotlin
dependencies {
    implementation(platform("org.http4k:http4k-bom:4.41.1.1"))
    implementation("org.http4k:http4k-contract-ui-swagger")
}
```

### Example [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/howto/create_a_swagger_ui/webjarExample.kt)

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/howto/create_a_swagger_ui/webjarExample.kt"></script>
