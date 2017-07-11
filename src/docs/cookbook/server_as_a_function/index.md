This example is the simplest possible "server" implementation. Note that we are not spinning up a server-backend here - but the entire application(!) is testable but firing HTTP requests at it as if it were. 

### Gradle setup
```
    compile group: "org.http4k", name: "http4k-core", version: "2.11.3"
```

### Code

<script src="http://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/cookbook/server_as_a_function/example.kt"></script>
