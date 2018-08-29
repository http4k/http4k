package cookbook.micro_apps

import org.http4k.client.JavaHttpClient
import org.http4k.core.then
import org.http4k.filter.DebuggingFilters.PrintRequestAndResponse
import org.http4k.filter.RequestFilters.ProxyHost
import org.http4k.filter.TrafficFilters.RecordTo
import org.http4k.routing.ResourceLoader.Companion.Directory
import org.http4k.routing.static
import org.http4k.server.SunHttp
import org.http4k.server.asServer
import org.http4k.traffic.Sink.Companion.DiskTree

fun `traffic sniffing proxy`() {
    PrintRequestAndResponse()
            .then(ProxyHost())
            .then(JavaHttpClient())
            .asServer(SunHttp(8000))
            .start()
            .block()
}

fun `recording traffic to disk proxy`() {
    RecordTo(DiskTree())
            .then(ProxyHost())
            .then(JavaHttpClient())
            .asServer(SunHttp(9000))
            .start()
            .block()
}

fun `static file server`() {
    static(Directory())
            .asServer(SunHttp(10000))
            .start()
            .block()
}

fun main(args: Array<String>) {
    `static file server`()
}
