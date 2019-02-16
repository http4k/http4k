title: http4k XML messaging Module
description: Feature overview of the http4k-format-xml module, which includes a limited automarshalling capability

### Installation (Gradle)
```compile group: "org.http4k", name: "http4k-format-xml", version: "3.113.0"```

### About
This module adds the ability to use XML as a first-class citizen when reading from and to HTTP messages. It also adds an 
automarshalling facility to read XML node objects directly into a custom type which matches the schema:

#### Code [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/modules/xml/autoXml.kt)

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/modules/xml/autoXml.kt"></script>

There is a utility to generate data class code from XML input [here](http://http4k-data-class-gen.herokuapp.com) . These data classes are compatible with using the `Body.auto<T>()` functionality. 
