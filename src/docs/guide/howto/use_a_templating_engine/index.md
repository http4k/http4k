title: http4k How-to: Use a templating engine
description: Recipes for using server-side templating engines with http4k applications, including hot-reload functionality

Example showing how to use the Templating modules - in this case Handlebars, both by standard response manipulation and via a typesafe view lens.

### Gradle setup

```kotlin
dependencies {
    implementation(platform("org.http4k:http4k-bom:5.6.1.0"))
    implementation("org.http4k:http4k-core")
    implementation("org.http4k:http4k-template-handlebars")
}
```

### Code [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/howto/use_a_templating_engine/example.kt)

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/howto/use_a_templating_engine/example.kt"></script>
