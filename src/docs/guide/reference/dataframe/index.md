title: http4k DataFrame Message Format Modules
description: Feature overview of the DataFrame http4k-format module, allowing for automatic extraction of DataFrames from HTTP messages.

### Installation (Gradle)

```kotlin
dependencies {
    implementation(platform("org.http4k:http4k-bom:5.21.1.0"))
 
    implementation("org.http4k:http4k-format-dataframe")
}
```

### About
These modules add the ability to use DataFrame as a first-class citizen when reading from HTTP messages. 

#### Code [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/reference/json/example.kt)

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/reference/dataframe/example.kt"></script>

[http4k]: https://http4k.org
