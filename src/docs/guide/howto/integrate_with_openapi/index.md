title: http4k How-to: Integrate with OpenAPI
description: Recipes for using the http4k-contract module to provide typesafe endpoints with automatically generated OpenAPI documentation

This contract example shows:

- 2 endpoints with typesafe contracts (marshalling of path parameters and bodies)
- Custom filters (reporting latency)
- API key security via a typesafe Query parameter (this can be a header or a body parameter as well)
- A parameter lens that provides metadata for the output OpenApi schema node
- OpenApi v3 documentation - Run this example and point a browser [here](https://http4k.org/openapi3?url=http%3A%2F%2Flocalhost%3A8000%2Fcontext%2Fdocs%2Fopenapi.json)

### Gradle setup

```kotlin
dependencies {
    implementation(platform("org.http4k:http4k-bom:5.14.4.0"))
    implementation("org.http4k:http4k-core")
    implementation("org.http4k:http4k-contract")
    implementation("org.http4k:http4k-format-argo")
}
```

Note: although we use Argo here as our JSON API, you could also switch in any of the `http4k-format-xxx` JSON modules. 

### Code [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/howto/integrate_with_openapi/example.kt)

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/howto/integrate_with_openapi/example.kt"></script>
