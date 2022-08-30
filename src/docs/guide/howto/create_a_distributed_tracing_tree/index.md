title: http4k How-to: Create a Distributed Tracing Tree
description: Recipes for creating a Distributed Tracing Tree

### Gradle setup

```groovy
implementation group: "org.http4k", name: "http4k-core", version: "4.30.3.0"
```

When composing several http4k services together and talking to Fakes representing external systems, we can create a tree of Distributed Trace calls by adding RequestTracing filters to our HTTP services and then tracking these trace calls through Events.

This tree can then be used to show the flow of traffic through the various services and to be used for things like documentation.

#### Code [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/howto/create_a_distributed_tracing_tree/example.kt)

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/howto/create_a_distributed_tracing_tree/example.kt"></script>
