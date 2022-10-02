package blog.nanoservices

import org.http4k.chaos.ChaosBehaviours.Latency
import org.http4k.chaos.ChaosEngine
import org.http4k.chaos.withChaosApi
import org.http4k.client.JavaHttpClient
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.then
import org.http4k.filter.RequestFilters.ProxyHost
import org.http4k.filter.RequestFilters.ProxyProtocolMode.Https
import org.http4k.server.SunHttp
import org.http4k.server.asServer
import java.lang.System.setProperty

fun `latency injection proxy (between 100ms-500ms)`() =
    ProxyHost(Https)
        .then(JavaHttpClient())
        .withChaosApi(ChaosEngine(Latency()).enable())
        .asServer(SunHttp())
        .start()

fun main() {
    setProperty("http.proxyHost", "localhost")
    setProperty("http.proxyPort", "8000")
    setProperty("http.nonProxyHosts", "localhost")

    `latency injection proxy (between 100ms-500ms)`().use {
        println(
            JavaHttpClient()(
                Request(POST, "http://localhost:8000/chaos/activate")
            )
        )
        println(
            JavaHttpClient()(
                Request(GET, "http://github.com/")
            ).header("X-http4k-chaos")
        )
    }
}
