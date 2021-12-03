package blog.nanoservices

import org.http4k.client.JavaHttpClient
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.then
import org.http4k.filter.RequestFilters.ProxyHost
import org.http4k.filter.RequestFilters.ProxyProtocolMode.Https
import org.http4k.filter.ResponseFilters.ReportRouteLatency
import org.http4k.server.SunHttp
import org.http4k.server.asServer
import java.lang.System.setProperty

fun `latency reporting proxy`() =
    ProxyHost(Https)
        .then(ReportRouteLatency { req, ms -> println("$req took $ms") })
        .then(JavaHttpClient())
        .asServer(SunHttp())
        .start()

fun main() {
    setProperty("http.proxyHost", "localhost")
    setProperty("http.proxyPort", "8000")
    setProperty("http.nonProxyHosts", "localhost")

    `latency reporting proxy`().use {
        JavaHttpClient()(Request(GET, "http://github.com/"))
    }
}
