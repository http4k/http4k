title: http4k Multipart Forms
description: Recipes for using http4k with Multipart forms

Multipart form support is provided on 2 levels:

1. Through the creation of a `MultipartFormBody` which can be set on a `Request`.
1. Using the Lens system, which adds the facility to define form fields in a typesafe way, and to validate form contents (in either a strict (400) or "feedback" mode).

### Gradle setup

```groovy
implementation group: "org.http4k", name: "http4k-core", version: "3.271.0"
implementation group: "org.http4k", name: "http4k-multipart", version: "3.271.0"
```

### Standard (non-typesafe) API [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/cookbook/multipart_forms/example_standard.kt)

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/cookbook/multipart_forms/example_standard.kt"></script>

### Lens (typesafe, validating) API - reads ALL contents onto disk/memory [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/cookbook/multipart_forms/example_lens.kt)

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/cookbook/multipart_forms/example_lens.kt"></script>

### Streaming - iterate over Multiparts [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/cookbook/multipart_forms/example_streaming.kt)

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/cookbook/multipart_forms/example_streaming.kt"></script>

### Processing Files with a Filter and convert to standard form [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/cookbook/multipart_forms/example_processing.kt)

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/cookbook/multipart_forms/example_processing.kt"></script>
