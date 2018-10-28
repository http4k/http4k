package guide.modules.cloud_native

import org.http4k.client.JavaHttpClient
import org.http4k.cloudnative.Http4kK8sServer
import org.http4k.cloudnative.asK8sServer
import org.http4k.cloudnative.env.Environment
import org.http4k.cloudnative.env.EnvironmentKey
import org.http4k.core.*
import org.http4k.core.Method.GET
import org.http4k.core.Status.Companion.OK
import org.http4k.filter.ClientFilters
import org.http4k.filter.DebuggingFilters
import org.http4k.lens.Lens
import org.http4k.server.SunHttp
import org.http4k.server.asServer

object ProxyApp {

    operator fun invoke(env: Environment): Http4kK8sServer {
        val otherServiceUri: Lens<Environment, Uri> = EnvironmentKey.k8s.serviceUriFor("otherservice")

        val proxyApp = ClientFilters.SetHostFrom(otherServiceUri(env))
            .then(rewriteUriToLocalhostAsWeDoNotHaveDns)
            .then(JavaHttpClient())

        return proxyApp.asK8sServer(::SunHttp, env)
    }

    private val rewriteUriToLocalhostAsWeDoNotHaveDns = Filter { next ->
        {
            println("Rewriting ${it.uri} so we can proxy properly")
            next(it.uri(it.uri.authority("localhost:9000")))
        }
    }
}

val enviromnentToUseInReality = Environment.JVM_PROPERTIES overrides Environment.ENV

val environmentSetByK8s = Environment.from(
    "SERVICE_PORT" to "8000",
    "HEALTH_PORT" to "8001",
    "OTHERSERVICE_SERVICE_PORT" to "9000"
)

fun main(args: Array<String>) {

    // start the other service
    { _: Request -> Response(OK).body("HELLO!") }.asServer(SunHttp(9000)).start().use {

        // start our service with the environment set by K8S
        ProxyApp(environmentSetByK8s).start().use {
            val client = DebuggingFilters.PrintResponse().then(JavaHttpClient())

            // health checks
            client(Request(GET, "http://localhost:8001/liveness"))
            client(Request(GET, "http://localhost:8001/readiness"))

            // proxied call
            client(Request(GET, "http://localhost:8000"))
        }
    }


}