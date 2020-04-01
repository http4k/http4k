package cookbook.monitoring

import org.http4k.core.HttpHandler
import org.http4k.core.HttpTransaction
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.filter.HttpTransactionLabeller
import org.http4k.filter.ResponseFilters
import org.http4k.routing.bind
import org.http4k.routing.routes
import java.time.Duration

fun main() {

    val app = routes("foo/{name}" bind { _: Request -> Response(OK) })

    fun metricConsumer(name: String, time: Duration) = println("$name ${time.toMillis()}ms")

    // this is a general use filter for reporting on http transactions
    val standardFilter = ResponseFilters.ReportHttpTransaction { tx: HttpTransaction ->
        metricConsumer("txLabels are: ${tx.labels}", tx.duration)
        metricConsumer("uri is: ${tx.request.uri}", tx.duration)
    }

    val addCustomLabels: HttpTransactionLabeller = { tx: HttpTransaction -> tx.label("status", tx.response.status.code.toString()) }

    val withCustomLabels = ResponseFilters.ReportHttpTransaction(
        transactionLabeller = addCustomLabels) { tx: HttpTransaction ->
        // send metrics to some custom system here...
        println("custom txLabels are: ${tx.labels} ${tx.duration}")
    }

    // this filter provides an anonymous identifier of the route
    val identifiedRouteFilter = ResponseFilters.ReportRouteLatency { requestGroup: String, duration: Duration ->
        metricConsumer("requestGroup is: " + requestGroup, duration)
    }

    val monitoredApp: HttpHandler = standardFilter
        .then(withCustomLabels)
        .then(identifiedRouteFilter)
        .then(app)

    monitoredApp(Request(GET, "/foo/bob"))

//    prints...
//    requestGroup is: GET.foo_{name}.2xx.200 7ms
//    custom txLabels are: {routingGroup=foo/{name}, status=200} PT0.05S
//        txLabels are: {routingGroup=foo/{name}} 51ms
//    uri is: /foo/bob 51ms
}
