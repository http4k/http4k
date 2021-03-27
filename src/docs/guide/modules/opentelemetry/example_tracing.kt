package guide.modules.opentelemetry

import io.opentelemetry.context.propagation.ContextPropagators.create
import io.opentelemetry.extension.aws.AwsXrayPropagator
import io.opentelemetry.sdk.OpenTelemetrySdk
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
    // configure OpenTelemetry using the Amazon XRAY tracing scheme
    val openTelemetry = OpenTelemetrySdk.builder()
        .setPropagators(create(AwsXrayPropagator.getInstance()))
        .buildAndRegisterGlobal()

    // this HttpHandler represents a 3rd party service, and will repeat the request body
    val repeater: HttpHandler = {
        println("REMOTE REQUEST WITH TRACING HEADERS: $it")
        Response(OK).body(it.bodyString() + it.bodyString())
    }

    // we will propagate the tracing headers using the tracer instance
    val repeaterClient = ClientFilters.OpenTelemetryTracing(openTelemetry).then(repeater)

    // this is the server app which will add tracing spans to incoming requests
    val app = ServerFilters.OpenTelemetryTracing(openTelemetry)
        .then(routes("/echo/{name}" bind GET to {
            val remoteResponse = repeaterClient(
                Request(POST, "http://aRemoteServer/endpoint")
                    .body(it.path("name")!!)
            )
            Response(OK).body(remoteResponse.bodyString())
        }))

    println("RETURNED TO CALLER: " + app(Request(GET, "http://localhost:8080/echo/david")))
}
