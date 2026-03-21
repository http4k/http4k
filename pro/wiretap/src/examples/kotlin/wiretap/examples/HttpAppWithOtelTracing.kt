/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package wiretap.examples

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.baggage.Baggage
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.Tracer
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.events.AutoOpenTelemetryEvents
import org.http4k.events.Event
import org.http4k.events.EventFilters
import org.http4k.events.Events
import org.http4k.events.HttpEvent
import org.http4k.events.then
import org.http4k.filter.ClientFilters
import org.http4k.filter.OpenTelemetryTracing
import org.http4k.filter.ResponseFilters
import org.http4k.filter.ServerFilters
import org.http4k.format.Moshi
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes
import java.time.Instant

fun HttpAppWithOtelTracing(
    downstreamUri: Uri,
    httpClient: HttpHandler,
    openTelemetry: OpenTelemetry = GlobalOpenTelemetry.get()
): RoutingHttpHandler {
    val tracer = openTelemetry.tracerProvider.get("demo-app")

    val events = EventFilters.AddServiceName("demo-app")
        .then(EventFilters.AddEventName())
        .then(AutoOpenTelemetryEvents(Moshi, openTelemetry))

    val client = ClientFilters.SetBaseUriFrom(downstreamUri)
        .then(ClientFilters.OpenTelemetryTracing(openTelemetry))
        .then(ResponseFilters.ReportHttpTransaction { events(HttpEvent.Outgoing(it)) })
        .then(httpClient)

    return ServerFilters.OpenTelemetryTracing(openTelemetry)
        .then(ResponseFilters.ReportHttpTransaction { events(HttpEvent.Incoming(it)) })
        .then(AppRoutes(tracer, events, client))
}

private fun AppRoutes(tracer: Tracer, events: Events, client: HttpHandler) = routes(
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

                val eventAttrs = Attributes.builder()
                    .put(AttributeKey.stringKey("foo"), "bar")
                    .put(AttributeKey.booleanKey("baz"), true)
                    .put(AttributeKey.longArrayKey("quux"), listOf(1L, 2L, 3L))
                    .put(AttributeKey.stringKey("waldo"), "fred")
                    .build()
                validateSpan.addEvent("event name 1", eventAttrs, Instant.now())
                validateSpan.addEvent("event name 2", eventAttrs, Instant.now())
                validateSpan.end()

                val cacheSpan = tracer.spanBuilder("cache-lookup")
                    .setSpanKind(SpanKind.INTERNAL)
                    .startSpan()
                cacheSpan.setAttribute("cache.type", "redis")
                cacheSpan.setAttribute("cache.hit", false)
                baggage.forEach { key, value -> cacheSpan.setAttribute("baggage.$key", value.value) }
                cacheSpan.addEvent("cache-miss")
                cacheSpan.end()

                events(AnEvent("event1", Instant.now(), 123, true))

                // make a call downstream
                client(req)

                events(AnEvent("event2", Instant.now(), 321, false))

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

data class AnEvent(val bar: String, val anotherDate: Instant, val aNumber: Int, val aBool: Boolean) : Event
