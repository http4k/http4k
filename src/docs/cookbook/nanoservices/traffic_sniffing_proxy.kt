package cookbook.nanoservices

import org.http4k.client.JavaHttpClient
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.then
import org.http4k.filter.DebuggingFilters.PrintRequestAndResponse
import org.http4k.filter.RequestFilters.ProxyHost
import org.http4k.filter.RequestFilters.ProxyProtocolMode.Https
import org.http4k.server.SunHttp
import org.http4k.server.asServer

fun `traffic sniffing proxy`() =
    ProxyHost(Https)
            .then(PrintRequestAndResponse())
            .then(JavaHttpClient())
            .asServer(SunHttp())
            .start()

fun main(args: Array<String>) {
    System.setProperty("http.proxyHost", "localhost")
    System.setProperty("http.proxyPort", "8000")
    System.setProperty("http.nonProxyHosts", "localhost")

    `traffic sniffing proxy`().use {
        JavaHttpClient()(Request(GET, "http://github.com/http4k"))
    }
}
