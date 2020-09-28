title: http4k Templating APIs
description: Recipes for using server-side templating engines with http4k applications, including hot-reload functionality

Example showing how to use the Templating modules - in this case Handlebars, both by standard response manipulation and via a typesafe view lens.

### Gradle setup

```groovy
implementation group: "org.http4k", name: "http4k-core", version: "3.262.0"
implementation group: "org.http4k", name: "http4k-template-handlebars", version: "3.262.0"
```

### Code [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/cookbook/using_templates/example.kt)

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/cookbook/using_templates/example.kt"></script>
