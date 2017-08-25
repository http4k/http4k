### Installation (Gradle)
**Argo:**  ```compile group: "org.http4k", name: "http4k-format-argo", version: "2.22.0"```

**Gson:**  ```compile group: "org.http4k", name: "http4k-format-gson", version: "2.22.0"```

**Jackson:** ```compile group: "org.http4k", name: "http4k-format-jackson", version: "2.22.0"```

### About
These modules add the ability to use JSON as a first-class citizen when reading from and to HTTP messages. Each implementation adds a set of 
standard methods and extension methods for converting common types into native JSON objects, including custom Lens methods for each library so that 
JSON node objects can be written and read directly from HTTP messages:

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/modules/message_formats/example.kt"></script>

### Auto-marshalling capabilities

Some of the JSON message libraries (eg. GSON and Jackson) provide the mechanism to automatically marshall data objects to/from JSON using reflection.

We can use this facility in **http4k** to automatically marshall objects to/from HTTP message bodies using **Lenses*:

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/modules/message_formats/auto.kt"></script>

#### Important note regarding JSON arrays
When handling raw JSON array messages, such as: `[123, 456, 567]`, there is a slight gotcha when auto-marshalling messages from JSON.

This is demonstrated by the following, where you can see that the output of the auto-unmarshalling a naked JSON is NOT the same as a native Kotlin list of objects. This can make tests break as the unmarshalled list is NOT equal to the native list.

As shown, a workaround to this is to use `Body.auto<Array<MyIntWrapper>>().toLens()` instead, and then compare using `Arrays.equal()`

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/modules/message_formats/list_gotcha.kt"></script>

