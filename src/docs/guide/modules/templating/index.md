title: http4k Templating Modules
description: Feature overview of the http4k-templating modules

### Installation (Gradle)

**Dust:**

```groovy
implementation group: "org.http4k", name: "http4k-template-dust", version: "3.154.1"
```

**Freemarker:**

```groovy
implementation group: "org.http4k", name: "http4k-template-freemarker", version: "3.154.1"
```

**Handlebars:**

```groovy
implementation group: "org.http4k", name: "http4k-template-handlebars", version: "3.154.1"
```

**Pebble:**

```groovy
implementation group: "org.http4k", name: "http4k-template-pebble", version: "3.154.1"
```

**Thymeleaf:**

```groovy
implementation group: "org.http4k", name: "http4k-template-thymeleaf", version: "3.154.1"
```

### About
The [http4k] templating API provides a standard mechanism for rendering using common templating libraries. Simply implement the `ViewModel` interface on a model class and pass it to the renderer to get a string. All of the implementations support view rendering using the following strategies:

* Cached on the classpath
* Cached from the filesystem
* Hot-Reloading from the filesystem

The examples below are for Handlebars, but the others have the same APIs:

#### Code  [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/modules/templating/example.kt)

 <script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/modules/templating/example.kt"></script>

[http4k]: https://http4k.org
