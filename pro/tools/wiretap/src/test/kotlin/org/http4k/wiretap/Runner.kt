package org.http4k.wiretap

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.baggage.Baggage
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.Tracer
import org.http4k.client.JavaHttpClient
import org.http4k.contract.bindContract
import org.http4k.contract.contract
import org.http4k.contract.openapi.ApiInfo
import org.http4k.contract.openapi.v3.OpenApi3
import org.http4k.core.ContentType
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.filter.OpenTelemetryTracing
import org.http4k.filter.ServerFilters
import org.http4k.lens.contentType
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.Jetty
import org.http4k.server.asServer
import org.http4k.server.uri

fun App2() = { request: Request ->
    Response(OK).headers(request.headers)
        .contentType(ContentType.APPLICATION_JSON).body("""{"hello":"world"}""")
}

fun App(
    uri: Uri,
    http: HttpHandler,
    openTelemetry: OpenTelemetry = GlobalOpenTelemetry.get()
): RoutingHttpHandler {
    val tracer = openTelemetry.tracerProvider.get("demo-app")

    val client = ClientFilters.SetBaseUriFrom(uri)
        .then(ClientFilters.OpenTelemetryTracing(openTelemetry)).then(http)

    return ServerFilters.OpenTelemetryTracing(openTelemetry).then(AppRoutes(tracer, client))
}

private fun AppRoutes(tracer: Tracer, client: HttpHandler) = routes(
    contract {
        renderer = OpenApi3(ApiInfo("App", "1.0"))
        descriptionPath = "/openapi"
        routes += "foo" bindContract GET to { _ -> Response(OK).body("bar") }
        routes += "bar" bindContract POST to { _ -> Response(OK).body("bar") }
    },
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
            Thread.sleep(2)
            cacheSpan.addEvent("cache-miss")
            cacheSpan.end()

            client(req)

            val response = client(req)

            val transformSpan = tracer.spanBuilder("transform-response")
                .setSpanKind(SpanKind.INTERNAL)
                .startSpan()
            transformSpan.setAttribute("transform.format", "json")
            transformSpan.setAttribute("transform.fields", 3)
            baggage.forEach { key, value -> transformSpan.setAttribute("baggage.$key", value.value) }
            Thread.sleep(3)
            transformSpan.addEvent("transform-complete")
            transformSpan.end()

            response
        }
})

fun main() {
    val app2 = App2().asServer(Jetty(0)).start()
    val server = Wiretap.Http { http, oTel, _ -> App(app2.uri(), http, oTel) }
        .asServer(Jetty(21000)).start()

    println("started ${server.uri().path("__wiretap")}")

    val client = JavaHttpClient()

    client(Request(GET, server.uri()))
    client(Request(GET, server.uri()))
    client(Request(GET, server.uri()))
}
