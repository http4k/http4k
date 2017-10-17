Example showing how to create and apply lenses to requests and responses to both extract and inject typesafe values out of and into HTTP messages. Note that since the **http4k** `Request/Response` objects are immutable, all injection occurs via copy.

### Gradle setup
```
    compile group: "org.http4k", name: "http4k-core", version: "2.33.1"
```

### Code
<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/cookbook/typesafe_http_requests_with_lenses/example.kt"></script>
