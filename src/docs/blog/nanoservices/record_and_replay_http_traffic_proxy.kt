package blog.nanoservices

import org.http4k.client.JavaHttpClient
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.then
import org.http4k.filter.RequestFilters.ProxyHost
import org.http4k.filter.RequestFilters.ProxyProtocolMode.Https
import org.http4k.filter.TrafficFilters.RecordTo
import org.http4k.server.SunHttp
import org.http4k.server.asServer
import org.http4k.traffic.ReadWriteStream.Companion.Disk
import java.lang.System.setProperty

fun `recording traffic to disk proxy`() =
    ProxyHost(Https)
        .then(RecordTo(Disk("store")))
        .then(JavaHttpClient())
        .asServer(SunHttp())
        .start()

fun `replay previously recorded traffic from a disk store`() =
    JavaHttpClient().let { client ->
        Disk("store").requests()
            .forEach {
                println(it)
                client(it)
            }
    }

fun main() {
    setProperty("http.proxyHost", "localhost")
    setProperty("http.proxyPort", "8000")
    setProperty("http.nonProxyHosts", "localhost")

    `recording traffic to disk proxy`().use {
        JavaHttpClient()(Request(GET, "http://github.com/"))
        JavaHttpClient()(Request(GET, "http://github.com/http4k"))
        JavaHttpClient()(Request(GET, "http://github.com/http4k/http4k"))
    }

    `replay previously recorded traffic from a disk store`()
}
