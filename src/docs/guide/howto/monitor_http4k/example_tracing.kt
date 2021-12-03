package guide.howto.monitor_http4k

import org.http4k.client.ApacheClient
import org.http4k.core.HttpHandler
import org.http4k.core.HttpMessage
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.filter.ResponseFilters
import org.http4k.filter.ServerFilters
import org.http4k.server.SunHttp
import org.http4k.server.asServer

fun main() {

    fun HttpMessage.logHeader(name: String) = "\n\t\t$name=${header(name)}"
    fun HttpMessage.traces() = logHeader("x-b3-traceid") + logHeader("x-b3-spanid") + logHeader("x-b3-parentspanid")

    fun audit(name: String) = ResponseFilters.ReportHttpTransaction { tx ->
        println("$name: ${tx.request.uri}\n\trequest:${tx.request.traces()}\n\tresponse:${tx.response.traces()}")
    }

    // a simple proxy to another app
    fun proxy(name: String, port: Int): HttpHandler {
        val proxyClient = ClientFilters.RequestTracing()
            .then(ClientFilters.SetHostFrom(Uri.of("http://localhost:$port")))
            .then(audit("$name-client"))
            .then(ApacheClient())

        return ServerFilters.RequestTracing().then(audit("$name-server")).then { proxyClient(Request(GET, it.uri)) }
    }

    // provides a simple ping
    fun ping(): HttpHandler = ServerFilters.RequestTracing().then(audit("ping-server")).then { Response(OK).body("pong") }

    val proxy1 = proxy("proxy1", 8001).asServer(SunHttp(8000)).start()
    val proxy2 = proxy("proxy2", 8002).asServer(SunHttp(8001)).start()
    val server3 = ping().asServer(SunHttp(8002)).start()

    audit("client").then(ApacheClient())(Request(GET, "http://localhost:8000/ping"))

    proxy1.stop()
    proxy2.stop()
    server3.stop()
}
