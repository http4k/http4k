title: http4k How-to: Server-as-a-Function
description: The simplest example of an http4k application 

This example is the simplest possible "server" implementation. Note that we are not spinning up a server-backend here - but the entire application(!) is testable by firing HTTP requests at it as if it were.

### Gradle setup

```groovy
implementation group: "org.http4k", name: "http4k-core", version: "4.17.3.0"
```

### Code [<img class="octocat"/>](https://github.com/http4k/http4k/blob/master/src/docs/guide/howto/server_as_a_function/example.kt)

<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/guide/howto/server_as_a_function/example.kt"></script>
