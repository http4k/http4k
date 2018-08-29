package cookbook.micro_apps

import org.http4k.chaos.ChaosBehaviours.Latency
import org.http4k.chaos.ChaosTriggers.Always
import org.http4k.chaos.appliedWhen
import org.http4k.chaos.withChaosControls
import org.http4k.client.JavaHttpClient
import org.http4k.client.OkHttp
import org.http4k.core.then
import org.http4k.filter.DebuggingFilters.PrintRequestAndResponse
import org.http4k.filter.RequestFilters.ProxyHost
import org.http4k.filter.ResponseFilters.ReportRouteLatency
import org.http4k.filter.TrafficFilters.RecordTo
import org.http4k.routing.ResourceLoader
import org.http4k.routing.static
import org.http4k.server.SunHttp
import org.http4k.server.asServer
import org.http4k.traffic.Replay
import org.http4k.traffic.Sink

fun `simple proxy`() {
    ProxyHost()
            .then(JavaHttpClient())
            .asServer(SunHttp(8000))
            .start()
            .block()
}

fun `traffic sniffing proxy`() {
    PrintRequestAndResponse()
            .then(ProxyHost())
            .then(JavaHttpClient())
            .asServer(SunHttp(8000))
            .start()
            .block()
}

fun `latency injection proxy (between 100ms-500ms)`() {
    ProxyHost()
            .then(JavaHttpClient())
            .withChaosControls(Latency().appliedWhen(Always))
            .asServer(SunHttp(8000))
            .start()
            .block()
}

fun `latency reporting proxy`() {
    ProxyHost()
            .then(ReportRouteLatency { req, ms -> println("$req took $ms")})
            .then(OkHttp())
            .asServer(SunHttp(8000))
            .start()
            .block()
}

fun `recording traffic to disk proxy`() {
    ProxyHost()
            .then(RecordTo(Sink.DiskTree()))
            .then(JavaHttpClient())
            .asServer(SunHttp(8000))
            .start()
            .block()
}

fun `replay previously recorded traffic from a disk store`() {
    JavaHttpClient().let { client ->
        Replay.DiskStream().requests().forEach { client(it) }
    }
}

fun `static file server`() {
    static(ResourceLoader.Directory())
            .asServer(SunHttp(8000))
            .start()
            .block()
}