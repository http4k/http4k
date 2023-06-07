title: http4k How-to: Routing API (Advanced)
description: Recipes for using the http4k composable routing API, including serving static content

This is a fairly comprehensive example of the core-routing logic available: 

- Individual HTTP endpoints are represented as `HttpHandlers`.
- Binding an `HttpHandler` to a path and HTTP verb yields a `Route`.
- `Routes` can be combined together into a `RoutingHttpHandler`, which is both an `HttpHandler` and a`Router`.
- A `Router` is a selective request handler, which attempts to match a request. If it cannot, processing falls through to the next `Router` in the list.
- Routers can be combined together to form another `HttpHandler`
- Usage of supplied core library `Filters`
- Serving of static content using a `Classpath` resource loader
- Support for Single Page Applications using a `singlePageApp()` block - resources loaded from here are loaded from the underlying `ResourceLoader` or fallback to `/` (and passed to the SPA code)

### Dynamic Paths / Path Variables
As you would expect, http4k allows routes to include dynamic or variable elements in the matching path, and allows you to reference the variable in the Handler. For example:
```
"/book/{title}" bind GET to { req -> 
    Response.invoke(Status.OK).body(GetBookDetails(req.path("title")) 
}
"/author/{name}/latest" bind GET to { req -> 
    Response.invoke(Status.OK).body(GetAllBooks(author = req.path("name")).last()) 
}
```

By default, the variable(s) will match anything. However you can append the variable name with a RegEx expression to limit the matches.
```
// will match /book/978-3-16-148410-0 (i.e. only digits and dashes)
// /book/A%20Confederacy%20Of%20Dunces would return a 404 (Not Found)
"/book/{isbn:[\\d-]+}"

// will NOT match /sales/south or /sales/usa
"/sales/{region:(?:northeast|southeast|west|international)}" 
```

There are no pre-defined types such as `int` or `path` for matching but these are easy to replicate with RegEx's:
- string (excluding slashes) : `[^\\/]+` (note that Kotlin requires backslashes to be escaped, so `\w` in RegEx is expressed as `\\w` in Kotlin)
- int : `\\d+`
- float : `[+-]?([0-9]*[.])?[0-9]+` (this will match basic floats. Does not match exponents, or scientific notation)
- path : `.*`

Note that paths, not strings, will match by default. `"/news/{date}"` will match `www.example.com/news/2018/05/26`, making `request.path("date")` equal to `2018/05/26`. This may be exactly what you want, or it may produce unexpected results, depending on how your URLs are structured.

### Gradle setup

```kotlin
dependencies {
    implementation(platform("org.http4k:http4k-bom:4.47.2.0"))
    implementation("org.http4k:http4k-core")
}
```

### Code [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/howto/nestable_routes/example.kt)

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/howto/nestable_routes/example.kt"></script>

For the typesafe contract-style routing, refer to [this](/guide/howto/integrate_with_openapi/) recipe instead,
