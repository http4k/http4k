title: http4k How-to: Create a custom JSON marshaller
description: Recipes for using JSON in http4k applications with a variety of popular JSON APIS

### Gradle setup

```groovy
implementation group: "org.http4k", name: "http4k-core", version: "4.16.0.0"
implementation group: "org.http4k", name: "http4k-format-jackson", version: "4.16.0.0"
```

### Custom auto-mapping JSON configurations

**http4k** declares an extended set of "primitive" types which it can marshall out of the box - this includes the various http4k primitives (Uri, Status), as well as a bunch of common types from the JDK such as the DateTime classes and Exceptions. These primitives types cannot be marshalled as top-level JSON structures on their own so should be contained in a custom wrapper class before transmission.

You can declare your own custom marshaller by reimplementing the Json instance and adding mappings for your own types - either uni or bi-directional.

This ability to render custom types through different JSON marshallers allows API users to provide different "views" for different purposes - for example we may wish to hide the values of some fields in the output, as below:

#### Code [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/howto/create_a_custom_json_marshaller/example.kt)

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/howto/create_a_custom_json_marshaller/example.kt"></script>
