title: http4k Templating APIs
description: Recipes for using server-side templating engines with http4k applications, including hot-reload functionality

Example showing how to use the Templating modules - in this case Handlebars, both by standard response manipulation and via a typesafe view lens.

### Gradle setup
```
    compile group: "org.http4k", name: "http4k-core", version: "3.35.1"
    compile group: "org.http4k", name: "http4k-template-handlebars", version: "3.35.1"
```

### Code [<img class="octocat" src="/img/octocat-32.png"/>](https://github.com/http4k/http4k/blob/master/src/docs/cookbook/using_templates/example.kt)
<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/cookbook/using_templates/example.kt"></script>
