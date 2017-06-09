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

```kotlin
import org.http4k.client.ApacheClient
import org.http4k.core.Request
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.server.asServer

fun main(args: Array<String>) {

    val app = { request: Request -> Response(OK).body("Hello, ${request.query("name")}!") }

    val jettyServer = app.asServer(Jetty(9000)).start()

    val request = Request(Method.GET, "http://localhost:9000").query("name", "John Doe")

    val client = ApacheClient()

    println(client(request))

    jettyServer.stop()
}
```

# See it in action:
* [Cookbook example code](https://github.com/http4k/http4k/tree/master/src/test/kotlin/cookbook)
* [Todo backend (simple version)](https://github.com/http4k/http4k-todo-backend)
* [Todo backend (typesafe contract version)](https://github.com/http4k/http4k-contract-todo-backend)
* [TDD'd example application](https://github.com/http4k/http4k-contract-example-app)
* [Stage-by-stage example of development process (London TDD style)](https://github.com/http4k/http4k/tree/master/src/test/kotlin/worked_example)
