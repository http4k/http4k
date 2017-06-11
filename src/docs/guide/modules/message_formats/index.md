### Installation (Gradle)
**Argo:**  ```compile group: "org.http4k", name: "http4k-format-argo", version: "2.4.0"```

**Gson:**  ```compile group: "org.http4k", name: "http4k-format-gson", version: "2.4.0"```

**Jackson:** ```compile group: "org.http4k", name: "http4k-format-jackson", version: "2.4.0"```

### About
These modules add the ability to use JSON as a first-class citizen when reading from and to HTTP messages. Each implementation adds a set of 
standard methods and extension methods for converting common types into native JSON objects, including custom Lens methods for each library so that 
JSON node objects can be written and read directly from HTTP messages:

<script src="http://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/message_format_module_0.kt"></script>
