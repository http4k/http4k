title: http4k JSON APIs
description: Recipes for using JSON in http4k applications with a variety of popular JSON APIS

Example of how to use the JSON library API wrappers, in this case the module used is Jackson. **http4k** provides an identical interface for all JSON implementations.

### Gradle setup
```
    compile group: "org.http4k", name: "http4k-core", version: "3.229.0"
    compile group: "org.http4k", name: "http4k-format-jackson", version: "3.229.0"
```

### Code [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/cookbook/using_json/example.kt)
<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/cookbook/using_json/example.kt"></script>
