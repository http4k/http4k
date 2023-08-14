title: http4k GraphQL module
description: Feature overview of the http4k-graphql module.

### Installation (Gradle)

```kotlin
dependencies {
    implementation(platform("org.http4k:http4k-bom:5.6.5.0"))
    implementation("org.http4k:http4k-graphql")

    // for the example below you will also need this dependency...
    implementation("com.expediagroup:graphql-kotlin-schema-generator", version = "5.3.2"
}
```


### About
This module provides http4k integration for the excellent [GraphQL-java](https://www.graphql-java.com/) library, allowing you to either serve or consume [GraphQL] services using a simple adapter API.

As with the [ethos](/guide/concepts/rationale) of http4k, the uniform Server/Client GraphQLHandler model means that you can test applications entirely in-memory without binding to a port. Http4k also ships with a page serving the GraphQL playground which can be added as a simple route.

#### Code [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/reference/graphql/example.kt)

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/reference/graphql/example.kt"></script>

[http4k]: https://http4k.org
[GraphQL]: https://graphql.org
