package cookbook.nanoservices

import org.http4k.client.JavaHttpClient
import org.http4k.core.then
import org.http4k.filter.RequestFilters.ProxyHost
import org.http4k.filter.TrafficFilters.RecordTo
import org.http4k.server.SunHttp
import org.http4k.server.asServer
import org.http4k.traffic.Replay
import org.http4k.traffic.Sink

fun `recording traffic to disk proxy`() {
    ProxyHost()
            .then(RecordTo(Sink.DiskTree()))
            .then(JavaHttpClient())
            .asServer(SunHttp())
            .start()
}

fun `replay previously recorded traffic from a disk store`() {
    JavaHttpClient().let { client ->
        Replay.DiskStream().requests().forEach { client(it) }
    }
}

