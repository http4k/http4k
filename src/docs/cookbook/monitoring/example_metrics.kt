package cookbook.monitoring

import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.filter.ResponseFilters
import org.http4k.routing.bind
import org.http4k.routing.routes
import java.time.Duration

fun main(args: Array<String>) {

    val app = routes("/{name}" bind { _: Request -> Response(OK) })

    fun metricConsumer(name: String, time: Duration) = println("$name ${time.toMillis()}ms")

    // this is a general use filter for reporting on call latency
    val standardFilter = ResponseFilters.ReportLatency { req: Request, _: Response, duration: Duration ->
        metricConsumer(req.uri.toString(), duration)
    }

    // this filter provides an anonymous identifier of the route
    val identifiedRouteFilter = ResponseFilters.ReportRouteLatency(recordFn = ::metricConsumer)

    val monitoredApp: HttpHandler = standardFilter
        .then(identifiedRouteFilter)
        .then(app)

    monitoredApp(Request(Method.GET, "/foo"))

//    prints...
//    GET.{name}.2xx.200 7ms
//    /foo 41ms
}