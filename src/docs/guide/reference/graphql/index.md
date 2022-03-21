title: http4k GraphQL module
description: Feature overview of the http4k-graphql module.

### Installation (Gradle)

```groovy
implementation group: "org.http4k", name: "http4k-graphql", version: "4.25.4.1"

// for the example below you will also need this dependency...
implementation group: "com.expediagroup", name: "graphql-kotlin-schema-generator", version: "5.3.2"
```


### About
This module provides http4k integration for the excellent [GraphQL-java](https://www.graphql-java.com/) library, allowing you to either serve or consume [GraphQL] services using a simple adapter API.

As with the ethos of http4k, the uniform Server/Client GraphQLHandler model means that you can test applications entirely in-memory without binding to a port.

#### Code [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/reference/graphql/example.kt)

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/reference/graphql/example.kt"></script>

[http4k]: https://http4k.org
[GraphQL]: https://graphql.org
