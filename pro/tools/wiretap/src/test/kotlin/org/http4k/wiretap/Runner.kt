package org.http4k.wiretap

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.baggage.Baggage
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.Tracer
import org.http4k.ai.mcp.PromptResponse
import org.http4k.ai.mcp.ToolResponse.Ok
import org.http4k.ai.mcp.model.Domain
import org.http4k.ai.mcp.model.Prompt
import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.model.apps.Csp
import org.http4k.ai.mcp.model.apps.McpAppResourceMeta
import org.http4k.ai.mcp.model.apps.McpApps
import org.http4k.ai.mcp.protocol.ServerMetaData
import org.http4k.ai.mcp.protocol.withExtensions
import org.http4k.ai.mcp.server.capability.extension.RenderMcpApp
import org.http4k.ai.mcp.server.security.NoMcpSecurity
import org.http4k.ai.model.Role
import org.http4k.client.JavaHttpClient
import org.http4k.contract.bindContract
import org.http4k.contract.contract
import org.http4k.contract.meta
import org.http4k.contract.openapi.ApiInfo
import org.http4k.contract.openapi.v3.OpenApi3
import org.http4k.core.ContentType
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.PolyHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.filter.McpFilters
import org.http4k.filter.OpenTelemetryTracing
import org.http4k.filter.PolyFilters
import org.http4k.filter.ServerFilters
import org.http4k.lens.contentType
import org.http4k.routing.bind
import org.http4k.routing.mcpHttpStreaming
import org.http4k.routing.poly
import org.http4k.routing.routes
import org.http4k.security.BasicAuthSecurity
import org.http4k.server.Jetty
import org.http4k.server.asServer
import org.http4k.server.uri
import org.http4k.wiretap.Option.app
import org.http4k.wiretap.Option.externalMcpAppUrl
import org.http4k.wiretap.Option.mcpApp
import org.http4k.wiretap.Option.website

fun App2() = { request: Request ->
    Response(OK).headers(request.headers)
        .contentType(ContentType.APPLICATION_JSON).body("""{"hello":"world"}""")
}

fun ServerApp(
    uri: Uri,
    http: HttpHandler,
    openTelemetry: OpenTelemetry = GlobalOpenTelemetry.get()
): PolyHandler {
    val tracer = openTelemetry.tracerProvider.get("demo-app")

    val client = ClientFilters.SetBaseUriFrom(uri)
        .then(ClientFilters.OpenTelemetryTracing(openTelemetry)).then(http)

    return poly(
        ServerFilters.OpenTelemetryTracing(openTelemetry).then(AppRoutes(tracer, client))
    )
}

fun ExampleMcpApp(otel: OpenTelemetry, client: HttpHandler) = PolyFilters.OpenTelemetryTracing(otel)
    .then(
        mcpHttpStreaming(
            ServerMetaData("test mcp app", "0.0.0").withExtensions(McpApps),
            NoMcpSecurity,
            RenderMcpApp(
                name = "show_ui",
                description = "shows the UI",
                uri = Uri.of("ui://a-ui"),
                meta = McpAppResourceMeta(
                    csp = Csp(
                        resourceDomains = listOf(Domain.of("https://resource.com")),
                        connectDomains = listOf(Domain.of("https://connect.com")),
                        frameDomains = listOf(Domain.of("https://frame.com"))
                    )
                )
            ) {
                runCatching {
                    ClientFilters.OpenTelemetryTracing(otel).then(client)(Request(GET, "https://http4k.org/"))
                }

                "hello world"
            },
            Tool("non_app", "") bind { Ok("hello") },
            Prompt("prompt", "") bind { PromptResponse(Role.Assistant, "hello") },
            mcpFilter = McpFilters.OpenTelemetryTracing(openTelemetry = otel)
        )
    )

private fun AppRoutes(tracer: Tracer, client: HttpHandler) = routes(
    contract {
        renderer = OpenApi3(ApiInfo("My Great App", "1.0"))
        security = BasicAuthSecurity("") { true }
        descriptionPath = "/openapi"
        routes += "foo" meta {
            summary = "bar"
            description = "isn't this a nice endpoint"
            returning(OK)
        } bindContract GET to { _ -> Response(OK).body("bar") }

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
            transformSpan.addEvent("transform-complete")
            transformSpan.end()

            response
        }
})

enum class Option {
    mcpApp, app, website, externalMcpAppUrl
}

fun main() {
    val wiretap = wiretapFor(mcpApp)

    val server = wiretap.asServer(Jetty(21000)).start()
    println("started ${server.uri().path("_wiretap")}")

    val client = JavaHttpClient()

    client(Request(GET, server.uri()))
    client(Request(GET, server.uri()))
    client(Request(GET, server.uri()))
}

private fun wiretapFor(option: Option): PolyHandler = when (option) {
    mcpApp ->
        Wiretap { client, oTel, _ ->
            ExampleMcpApp(oTel, client).asServer(Jetty(0)).start().uri()
        }

    app -> {
        val clientApp = App2().asServer(Jetty(0)).start()

        Wiretap { http, oTel, _ ->
            ServerApp(clientApp.uri(), http, oTel).asServer(Jetty(0)).start().uri()
        }
    }

    website -> Wiretap(Uri.of("https://http4k.org"))
    externalMcpAppUrl -> Wiretap(Uri.of("https://demo.http4k.org/mcp-app/"))
}
