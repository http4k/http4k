### Installation (Gradle)
**Handlebars:** ```compile group: "org.http4k", name: "http4k-template-handlebars", version: "2.22.1"```

**Pebble:** ```compile group: "org.http4k", name: "http4k-template-pebble", version: "2.22.1"```

**Thymeleaf:** ```compile group: "org.http4k", name: "http4k-template-thymeleaf", version: "2.22.1"```

### About
The **http4k** templating API provides a standard mechanism for rendering using common templating libraries. Simply implement the `ViewModel` interface on a model class and pass it to the renderer to get a string. All of the implementations support view rendering using the following strategies:

* Cached on the classpath
* Cached from the filesystem
* Hot-Reloading from the filesystem

```kotlin
data class Person(val name: String, val age: Int) : ViewModel

val renderer = HandlebarsTemplates().HotReload("src/test/resources")

val app: HttpHandler = {
    val viewModel = Person("Bob", 45)
    val renderedView = renderer(viewModel)
    Response(OK).body(renderedView)
}

println(app(Request(Method.GET, "/someUrl")))
```
