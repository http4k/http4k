### Installation (Gradle)
**Handlebars:** ```compile group: "org.http4k", name: "http4k-template-handlebars", version: "2.18.5"```

**Pebble:** ```compile group: "org.http4k", name: "http4k-template-pebble", version: "2.18.5"```

**Thymeleaf:** ```compile group: "org.http4k", name: "http4k-template-thymeleaf", version: "2.18.5"```

### About
The pluggable **http4k** templating API adds `ViewModel` rendering for common templating libraries. The implementations provide the a number of renderers for views:
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
