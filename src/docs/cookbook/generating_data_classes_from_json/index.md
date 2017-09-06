This example show the usage of the `GenerateDataClasses` to generate Kotlin data class code for JSON messages from a remote endpoint. When used in conjunction with the "auto body-marshalling" functionality available with JSON libraries such as Jackson and GSON, this provides a super-fast way to integrate with upstream remote APIs in a typesafe way.

### Gradle setup
```
    compile group: "org.http4k", name: "http4k-core", version: "2.25.1"
```

### Code
<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/cookbook/generating_data_classes_from_json/example.kt"></script>
