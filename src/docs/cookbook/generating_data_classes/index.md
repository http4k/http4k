title: http4k Data Class Generation
description: How to use the http4k Lens system to automatically marshall HTTP messages on and off the wire

### On the web...
This [Heroku](http://http4k-data-class-gen.herokuapp.com/) app demonstrates how to use JSON and XML automarshalling to communicate using typesafe Body lenses.

### Or manually using a filter...
This example show the usage of the `GenerateDataClasses` to generate Kotlin data class code for JSON messages from a remote endpoint. When used in conjunction with the "auto body-marshalling" functionality available with JSON libraries such as Jackson and GSON, this provides a super-fast way to integrate with upstream remote APIs in a typesafe way.

### Gradle setup
```
    compile group: "org.http4k", name: "http4k-core", version: "3.237.0"
    compile group: "org.http4k", name: "http4k-format-gson", version: "3.237.0"
```

### Code [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/cookbook/generating_data_classes/example.kt)
<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/cookbook/generating_data_classes/example.kt"></script>
