This simple example demonstates how to serve and consume HTTP services using **http4k**. 

To install, add these dependencies to your **Gradle** file:
```groovy
dependencies {
    compile group: "org.http4k", name: "http4k-core", version: "X.X.X"
    compile group: "org.http4k", name: "http4k-server-jetty", version: "X.X.X"
    compile group: "org.http4k", name: "http4k-client-apache", version: "X.X.X"
}
```

The following creates a simple endpoint, binds it to a Jetty server then starts, queries, and stops it.

<script src="http://gist-it.appspot.com/https://github.com/http4k/http4k/blob/master/src/test/kotlin/site/quickstart0.kt"></script>

# See it in action:
* [Cookbook example code](https://github.com/http4k/http4k/tree/master/src/test/kotlin/cookbook)
* [Todo backend (simple version)](https://github.com/http4k/http4k-todo-backend)
* [Todo backend (typesafe contract version)](https://github.com/http4k/http4k-contract-todo-backend)
* [TDD'd example application](https://github.com/http4k/http4k-contract-example-app)
* [Stage-by-stage example of development process (London TDD style)](https://github.com/http4k/http4k/tree/master/src/test/kotlin/worked_example)
