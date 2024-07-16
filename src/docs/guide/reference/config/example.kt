package guide.reference.config

import org.http4k.client.JavaHttpClient
import org.http4k.config.Environment
import org.http4k.config.EnvironmentKey
import org.http4k.config.Secret
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.DebuggingFilters
import org.http4k.lens.secret
import org.http4k.lens.uri
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.Http4kServer
import org.http4k.server.SunHttp
import org.http4k.server.asServer

private val url = EnvironmentKey.uri().required("url")
private val secret = EnvironmentKey.secret().required("secret")

object App {

    operator fun invoke(env: Environment): Http4kServer {
        val app = routes(
            "/config" bind GET to {
                Response(OK).body(
                    """
                        url: ${url(env)}
                        secret: ${secret(env)}
                    """.trimIndent()
                )
            }
        )
        return app.asServer(SunHttp(8001))
    }
}

fun main() {
    val defaultConfig = Environment.defaults(
        url of Uri.of("http://localhost:9000"),
        secret of Secret("mysecret")
    )

    // standard chaining order for properties is local file -> JVM -> Environment -> defaults -> boom!
    val env = Environment.fromResource("app.properties") overrides
        Environment.JVM_PROPERTIES overrides
        Environment.ENV overrides
        defaultConfig

    val server = App(env).start()

    performHealthChecks()

    server.stop()
}

private fun performHealthChecks() {
    val client = DebuggingFilters.PrintResponse().then(JavaHttpClient())

    client(Request(GET, "http://localhost:8001/config"))
}
