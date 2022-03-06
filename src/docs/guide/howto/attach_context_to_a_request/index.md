title: http4k How-to: Request Contexts
description: Recipes for using http4k with per-request context objects

A `RequestContext` makes it possible to attach objects to a request whilst it is being passed down through the layers of an application.

The basic concept is that there is a global shared object which holds a bag of state (indexed by Request). This state can be modified in Filters and then 
that state accessed inside other Filters or the terminating HttpHandler. There are 2 available choices for manipulating this data:

1. Using simple Strings to represent the keys.
1. Using `RequestContextKey`s and the Lens mechanism from the `http4k-core` module.

Whilst the first method looks technically simpler, the use of simple Strings does not provide the type-safety of the second, which uses unique shared Key objects to guarantee non-clashing of keys and type-safety of the state.
Regardless of which of the above mechanisms are used, an instance of the `ServerFilters.InitialiseRequestContext` Filter must wrap the HttpHandler(s) to activate
the shared bag of state for each request, and to remove the state after the request is complete.

### Gradle setup

```groovy
implementation group: "org.http4k", name: "http4k-core", version: "4.20.1.0"
```

#### String-based keys [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/howto/attach_context_to_a_request/string_key_example.kt)

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/howto/attach_context_to_a_request/string_key_example.kt"></script>

#### Lens-based keys [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/howto/attach_context_to_a_request/lens_key_example.kt)

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/howto/attach_context_to_a_request/lens_key_example.kt"></script>
