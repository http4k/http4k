package wiretap.examples

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.baggage.Baggage
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.Tracer
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.filter.OpenTelemetryTracing
import org.http4k.filter.ServerFilters
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes

fun HttpAppWithOtelTracing(
    downstreamUri: Uri,
    httpClient: HttpHandler,
    openTelemetry: OpenTelemetry = GlobalOpenTelemetry.get()
): RoutingHttpHandler {
    val tracer = openTelemetry.tracerProvider.get("demo-app")

    val client = ClientFilters.SetBaseUriFrom(downstreamUri)
        .then(ClientFilters.OpenTelemetryTracing(openTelemetry)).then(httpClient)

    return ServerFilters.OpenTelemetryTracing(openTelemetry).then(AppRoutes(tracer, client))
}

private fun AppRoutes(tracer: Tracer, client: HttpHandler) = routes(
    "/{name:.*}" bind GET to { req ->
        Baggage.current()
            .toBuilder()
            .put("user.id", "user-42")
            .put("session.id", "sess-abc123")
            .build()
            .makeCurrent().use {
                val baggage = Baggage.current()

                val validateSpan = tracer.spanBuilder("validate-request")
                    .setSpanKind(SpanKind.INTERNAL)
                    .startSpan()
                validateSpan.setAttribute("validation.type", "auth-check")
                validateSpan.setAttribute("user.role", "admin")
                baggage.forEach { key, value -> validateSpan.setAttribute("baggage.$key", value.value) }
                Thread.sleep(5)
                validateSpan.addEvent("credentials-verified")
                validateSpan.end()

                val cacheSpan = tracer.spanBuilder("cache-lookup")
                    .setSpanKind(SpanKind.INTERNAL)
                    .startSpan()
                cacheSpan.setAttribute("cache.type", "redis")
                cacheSpan.setAttribute("cache.hit", false)
                baggage.forEach { key, value -> cacheSpan.setAttribute("baggage.$key", value.value) }
                cacheSpan.addEvent("cache-miss")
                cacheSpan.end()

                // make a call downstream
                client(req)

                // make another call downstream
                val response = client(req)

                val transformSpan = tracer.spanBuilder("transform-response")
                    .setSpanKind(SpanKind.INTERNAL)
                    .startSpan()
                transformSpan.setAttribute("transform.format", "json")
                transformSpan.setAttribute("transform.fields", 3)
                baggage.forEach { key, value -> transformSpan.setAttribute("baggage.$key", value.value) }
                transformSpan.addEvent("transform-complete")
                transformSpan.end()

                response
            }
    })
