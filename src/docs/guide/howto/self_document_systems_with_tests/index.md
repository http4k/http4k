title: http4k How-to: Self Document Systems with Tests
description: Recipes for creating a Distributed Tracing Tree

### Gradle setup

```groovy
implementation group: "org.http4k", name: "http4k-incubator", version: "4.40.1.0"
```

When composing several http4k services together and talking to Fakes representing external systems, we can use a combination of request tracing filters (utilising distributed tracing headers) and the http4k event stream to capture and record events. This stream can be stored to disk or outputted to various rendered formats such as [PlantUML] or [Mermaid]. 

#### Code [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/howto/self_document_systems_with_tests/example.kt)

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/howto/self_document_systems_with_tests/example.kt"></script>

[PlantUML]: https://plantuml.com/
[Mermaid]: https://mermaid.js.org/
