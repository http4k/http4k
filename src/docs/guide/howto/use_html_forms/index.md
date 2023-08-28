title: http4k How-to: Use HTML Forms
description: Recipes for using http4k with HTML forms

HTML form support is provided on 2 levels:

1. Through the use of `form()` extension methods on `Request` to get/set String values.
1. Using the Lens system, which adds the facility to define form fields in a typesafe way, and to validate form contents (in either a strict (400) or "feedback" mode).

### Gradle setup

```kotlin
dependencies {
    implementation(platform("org.http4k:http4k-bom:5.7.4.0"))
    implementation("org.http4k:http4k-core")
}
```

### Standard (non-typesafe) API [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/howto/use_html_forms/example_standard.kt)

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/howto/use_html_forms/example_standard.kt"></script>

### Lens (typesafe, validating) API [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/howto/use_html_forms/example_lens.kt)

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/howto/use_html_forms/example_lens.kt"></script>
