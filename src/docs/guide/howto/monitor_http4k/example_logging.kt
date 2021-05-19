package guide.howto.monitor_http4k

import org.http4k.core.HttpHandler
import org.http4k.core.HttpTransaction
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.filter.ResponseFilters
import org.http4k.routing.bind
import org.http4k.routing.routes
import java.time.Clock

fun main() {

    val app = routes("/{name}" bind { _: Request -> Response(OK) })

    fun logger(message: String) = println("${Clock.systemUTC().instant()} $message")

    val audit = ResponseFilters.ReportHttpTransaction { tx: HttpTransaction ->
        logger("my call to ${tx.request.uri} returned ${tx.response.status} and took ${tx.duration.toMillis()}")
    }

    val monitoredApp: HttpHandler = audit.then(app)

    monitoredApp(Request(GET, "/foo"))

//    prints...
//    2017-12-04T08:38:27.499Z my call to /foo returned 200 OK and took 5
}
