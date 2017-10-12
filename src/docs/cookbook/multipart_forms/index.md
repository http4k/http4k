Multipart form support is provided on 2 levels:

1. Through the creation of a `MultipartFormBody` which can be set on a `Request`.
1. Using the Lens system, which adds the facility to define form fields in a typesafe way, and to validate form contents (in either a strict (400) or "feedback" mode).

### Gradle setup
```
    compile group: "org.http4k", name: "http4k-core", version: "2.31.1"
    compile group: "org.http4k", name: "http4k-multipart", version: "2.31.1"
```

### Standard (non-typesafe) API
<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/cookbook/multipart_forms/example_standard.kt"></script>

### Lens (typesafe, validating) API
<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/cookbook/multipart_forms/example_lens.kt"></script>
