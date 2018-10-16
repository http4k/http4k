title: http4k Routing API (Advanced)
description: Recipes for using the http4k composable routing API, including serving static content

This is a fairly comprehensive example of the core-routing logic available:

- Individual HTTP endpoints are represented as `HttpHandlers`.
- Binding an `HttpHandler` to a path and HTTP verb yields a `Route`.
- `Routes` can be combined together into a `RoutingHttpHandler`, which is both an `HttpHandler` and a`Router`.
- A `Router` is a selective request handler, which attempts to match a request. If it cannot, processing falls through to the next `Router` in the list.
- Routers can be combined together to form another `HttpHandler`
- Usage of supplied core library `Filters`
- Serving of static content using a `Classpath` resource loader

### Gradle setup
```
    compile group: "org.http4k", name: "http4k-core", version: "3.39.1"
```

### Code [<img class="octocat" src="/img/octocat-32.png"/>](https://github.com/http4k/http4k/blob/master/src/docs/cookbook/nestable_routes/example.kt)

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/cookbook/nestable_routes/example.kt"></script>
