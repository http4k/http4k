title: http4k XML messaging Modules
description: Feature overview of the modules providing XML support, including an auto-marshalling capability

### Installation (Gradle)

```groovy
// json.org XML:
implementation group: "org.http4k", name: "http4k-format-xml", version: "3.270.1"

// Jackson XML:
implementation group: "org.http4k", name: "http4k-format-jackson-xml", version: "3.270.1"
```

### About
These modules provide auto-marshalling functionality to convert XML into arbitrary data classes. The 2 differ slightly in their behaviour, due to the underlying libraries used for implementation. We recommend using `http4k-format-jackson-xml` as it has more predictable and consistent behaviour.

### JacksonXml
- Provides bi-directional conversion
- Does NOT expose an XML DOM node model
- Generally requires `Jackson` field annotations to manipulate output format
- Provides extension point to map custom types using BiDiMapping registration (so supports all Java and Http4k primitives such as `Uri`)

#### JacksonXML Code [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/modules/xml/jacksonAutoXml.kt)
- Provides extraction conversion only
- Exposes an XML DOM node model as a first-class citizen - so can read directly from a string into a DOM model
- Does not generate a wrapper element to represent the parent node
- Has trouble with repeating child-elements, depending on zero, one or many elements in the XML. This is due to the underlying library implementation
- Only handles objects with primitive JDK types

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/modules/xml/jacksonAutoXml.kt"></script>

### Xml
As above, we recommend using `http4k-format-jackson-xml` as it has more predictable and consistent behaviour.
 
#### XML Code [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/modules/xml/autoXml.kt)

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/modules/xml/autoXml.kt"></script>

<!--
There is a utility to generate data class code from XML input [here](http://http4k-data-class-gen.herokuapp.com) . These data classes are compatible with using the `Body.auto<T>()` functionality. 
-->
