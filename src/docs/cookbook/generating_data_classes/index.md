### On the web...
This [Heroku](http://http4k-data-class-gen.herokuapp.com/) app demonstrates how to use JSON and XML automarshalling to communicate using typesafe Body lenses.

### Or manually using a filter...
This example show the usage of the `GenerateDataClasses` to generate Kotlin data class code for JSON messages from a remote endpoint. When used in conjunction with the "auto body-marshalling" functionality available with JSON libraries such as Jackson and GSON, this provides a super-fast way to integrate with upstream remote APIs in a typesafe way.

### Gradle setup
```
    compile group: "org.http4k", name: "http4k-core", version: "2.31.0"
    compile group: "org.http4k", name: "http4k-format-gson", version: "2.31.0"
```

### Code
<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/cookbook/generating_data_classes/example.kt"></script>
