title: http4k Clients
description: Recipes for using http4k to consume HTTP services

This example demonstrates a client module (in this case the Apache Client). A client is just another HttpHandler.

### Gradle setup
```
    compile group: "org.http4k", name: "http4k-core", version: "3.17.0"
    compile group: "org.http4k", name: "http4k-client-apache", version: "3.17.0"
```

### Code [<img class="octocat" src="/img/octocat-32.png"/>](https://github.com/http4k/http4k/blob/master/src/docs/cookbook/client_as_a_function/example.kt)
<script src="https://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/docs/cookbook/client_as_a_function/example.kt"></script>
