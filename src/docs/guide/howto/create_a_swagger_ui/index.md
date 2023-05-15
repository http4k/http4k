title: Redoc and Swagger UI
description: Create a Redoc or Swagger UI for your REST API

Http4k makes it easy to include Swagger UI or Redoc in your application.
These UIs can often replace traditional hand-written documentation for API consumers to learn your API,
and can even serve as useful debugging tools.

## Build the OpenAPI spec

Swagger UI and Redoc both require an **OpenApi** v2 or v3 description to function.
Http4k can generate a description for your API with the `http4k-contract` module,
but any hand-crafted or external description can be used as well.

For more detail on generating **OpenAPI** descriptions, see:

- [Http4k Reference: Contracts](/guide/reference/contracts)
- [http4k How-to: Integrate with OpenAPI](/guide/howto/integrate_with_openapi)

### Example [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/howto/create_a_swagger_ui/exampleContract.kt)

This simple description will be used for all examples in this guide:

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/howto/create_a_swagger_ui/exampleContract.kt"></script>

## Build the UI

The `http4-contract` module includes functions to configure and serve Swagger UI, Redoc, or both.
These "lite" UIs are thin; meaning most of the assets are pulled from an external Public CDN.

### Installation (Gradle)

```kotlin
dependencies {
    implementation(platform("org.http4k:http4k-bom:4.43.1.0"))
    implementation("org.http4k:http4k-contract")
}
```

### Example [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/howto/create_a_swagger_ui/exampleLite.kt)

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/howto/create_a_swagger_ui/exampleLite.kt"></script>

## Bundle the UI with Webjars

The "lite" UIs included in the `http4k-contract` module are great for serverless APIs, where binary size is a major concern.
For more control over the assets, http4k has optional modules to bundle the assets into your application.

### Installation (Gradle)

```kotlin
dependencies {
    implementation(platform("org.http4k:http4k-bom:4.43.1.0"))
    implementation("org.http4k:http4k-contract-ui-swagger")
    implementation("org.http4k:http4k-contract-ui-redoc")
}
```

You can pick and choose whether you want Redoc, Swagger UI, or both bundled with your application.

### Example [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/howto/create_a_swagger_ui/exampleWebjar.kt)

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/howto/create_a_swagger_ui/exampleWebjar.kt"></script>
