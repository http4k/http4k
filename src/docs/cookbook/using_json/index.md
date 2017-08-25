Example of how to use the JSON library API wrappers, in this case the module used is Jackson. **http4k** provides an identical interface for all JSON implementations.

### Gradle setup
```
    compile group: "org.http4k", name: "http4k-core", version: "2.22.1"
    compile group: "org.http4k", name: "http4k-format-jackson", version: "2.22.1"
```

### Code
<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/cookbook/using_json/example.kt"></script>
