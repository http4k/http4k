package guide.modules.opentelemetry

import io.opentelemetry.OpenTelemetry
import io.opentelemetry.context.propagation.DefaultContextPropagators
import io.opentelemetry.extensions.trace.propagation.AwsXRayPropagator
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.filter.OpenTelemetryTracing
import org.http4k.filter.ServerFilters
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.routes

fun main() {
    // configure OpenTelemetry using the Amazon XRAY trace propagator API
    val tracer = OpenTelemetry.getTracer("http4k", "semver:0.0.0")
    OpenTelemetry.setPropagators(
        DefaultContextPropagators.builder()
            .addTextMapPropagator(AwsXRayPropagator.getInstance())
            .build())

    // this HttpHandler represents the 3rd party service, and will repeat the request body
    val repeater: HttpHandler = {
        println("REMOTE REQUEST WITH TRACING HEADERS: $it")
        Response(OK).body(it.bodyString() + it.bodyString())
    }

    // we will propagate the tracing headers using the tracer instance
    val repeaterClient = ClientFilters.OpenTelemetryTracing(tracer).then(repeater)


    // this is the server app which will add tracing spans to incoming requests
    val app = ServerFilters.OpenTelemetryTracing(tracer)
        .then(routes("/echo/{name}" bind GET to {
            val remoteResponse = repeaterClient(
                Request(POST, "http://aRemoteServer/endpoint")
                    .body(it.path("name")!!)
            )
            Response(OK).body(remoteResponse.bodyString())
        }))

    println("RETURNED TO CALLER: " + app(Request(GET, "http://localhost:8080/echo/david")))
}
