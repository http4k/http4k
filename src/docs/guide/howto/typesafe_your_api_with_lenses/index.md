title: http4k How-to: Typesafe your API with lenses
description: Recipes for using the inbuilt http4k Lens API to marshall HTTP messages on and off the wire in a typesafe fashion

Example showing how to create and apply lenses to requests and responses to both extract and inject typesafe values out of and into HTTP messages. Note that since the **http4k** `Request/Response` objects are immutable, all injection occurs via copy.

### Gradle setup

```groovy
implementation group: "org.http4k", name: "http4k-core", version: "4.40.2.0"
```

### Standard (exception based) approach [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/howto/typesafe_your_api_with_lenses/example.kt)
Errors in extracting Lenses are propagated as exceptions which are caught and handled by the `CatchLensFailure` Filter.

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/howto/typesafe_your_api_with_lenses/example.kt"></script>

### Using "Result" ADT
An alternative approach to using Exceptions to automatically produce `BadRequests` is to use an Either-type structure, and this would be easy to implement - but the lack of an usable Result/Either type in the standard Kotlin library means that we have chosen to use `Result4k` as an optional dependency. If it is on the classpath you will gain support for it.

Additionally, the lack of Higher Kinded Types in Kotlin means that we are unable to provide a generic method for converting standard lenses. However, it is easy to implement an extension method to use in specific use cases - you can follow the example in the http4k source to implement your own version of the one we supply for Result4k. Below is an example which uses that Result4k ADT:

### Code [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/howto/typesafe_your_api_with_lenses/example_result4k.kt)

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/howto/typesafe_your_api_with_lenses/example_result4k.kt"></script>

