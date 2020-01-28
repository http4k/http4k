title: http4k Lens API
description: Recipes for using the inbuilt http4k Lens API to marshall HTTP messages on and off the wire in a typesafe fashion

Example showing how to create and apply lenses to requests and responses to both extract and inject typesafe values out of and into HTTP messages. Note that since the **http4k** `Request/Response` objects are immutable, all injection occurs via copy.

### Gradle setup
```
    compile group: "org.http4k", name: "http4k-core", version: "3.228.0"
```

### Standard (exception based) approach [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/cookbook/typesafe_http_requests_with_lenses/example.kt)
Errors in extracting Lenses are propagated as exceptions which are caught and handled by the `CatchLensFailure` Filter.

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/cookbook/typesafe_http_requests_with_lenses/example.kt"></script>

### Using custom "Result" ADTs
An alternative approach to using Exceptions to automatically produce `BadRequests` is to use an Either-type structure, and this would be easy to implement - but the lack of an in-built Result/Either type in the standard Kotlin library means that we 
don't have a single representation to use without shackling ourselves to another Either-containing library such as Arrow or Result4k.

Additionally, the lack of Higher Kinded Types in Kotlin means that we are unable to provide a generic method for converting standard lenses. However, it is easy to implement an extension method to use in specific use cases.

Below is an example which uses a custom Result ADT - this will work for all extraction Lenses that you define:

### Code [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/cookbook/typesafe_http_requests_with_lenses/example_custom_result_adt.kt)
<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/cookbook/typesafe_http_requests_with_lenses/example_custom_result_adt.kt"></script>

