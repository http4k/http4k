title: http4k KotlinX DataFrame Module
description: Feature overview of the DataFrame http4k-format module, allowing for automatic extraction of DataFrames from HTTP messages.

### Installation (Gradle)

```kotlin
dependencies {
    implementation(platform("org.http4k:http4k-bom:5.25.0.0"))
    implementation("org.http4k:http4k-format-dataframe")
}
```

### About
This module adds the ability to use [Kotlin DataFrames](https://kotlin.github.io/dataframe) as a first-class citizen when reading from HTTP messages. Extraction from the HTTP message body is done automatically when using a lens with a DataFrame type.

#### Code [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/reference/dataframe/example.kt)

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/reference/dataframe/example.kt"></script>

[http4k]: https://http4k.org
