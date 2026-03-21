/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.junit

import App
import RequestProcessed
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.greaterThan
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.baggage.Baggage
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.StatusCode
import org.http4k.ai.mcp.testing.testMcpClient
import org.http4k.client.JavaHttpClient
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.events.AutoOpenTelemetryEvents
import org.http4k.format.Moshi
import org.http4k.server.SunHttp
import org.http4k.server.asServer
import org.http4k.server.uri
import org.http4k.util.PortBasedTest
import org.http4k.wiretap.acceptance.orThrowIt
import org.http4k.wiretap.domain.Ordering.Descending
import org.http4k.wiretap.junit.RenderMode.Always
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import wiretap.examples.McpServerWithOtelTracing

class GlobalOTelInterceptTest : PortBasedTest {

    @RegisterExtension
    @JvmField
    val intercept = Intercept(renderMode = Always)

    @Test
    fun `http traces are captured via GlobalOpenTelemetry without explicit otel wiring`(http: HttpHandler) {
        val app = App(http)
        app(Request(GET, "/test"))

        val traces = intercept.traceStore.traces(Descending)
        assertThat(traces.size, greaterThan(0))
    }

    @Test
    fun `mcp traces are captured via GlobalOpenTelemetry without explicit otel wiring`() {
        McpServerWithOtelTracing({ _: Request -> Response(Status.OK) }).testMcpClient().use {
            it.tools().list().orThrowIt()
        }
    }

    @Test
    fun `with a running app`() {
        App().asServer(SunHttp(0)).start().use {
            JavaHttpClient()(Request(GET, it.uri().path("/test")))
            JavaHttpClient()(Request(GET, it.uri().path("/other")))
        }
    }

    @Test
    fun `with no app`() {
        val otel = GlobalOpenTelemetry.get()
        val tracer = otel.tracerProvider.get("standalone-service")
        val logger = otel.logsBridge.get("standalone-service")
        val events = AutoOpenTelemetryEvents(Moshi, otel)

        val parentSpan = tracer.spanBuilder("process-order").startSpan()
        parentSpan.makeCurrent().use {
            Baggage.current().toBuilder()
                .put("user.id", "user-99")
                .put("tenant.id", "acme-corp")
                .build()
                .makeCurrent().use {
                    Span.current().setAttribute("order.id", "order-456")
                    Span.current().addEvent("order-received")

                    logger.logRecordBuilder()
                        .setBody("Processing order order-456")
                        .setAttribute(AttributeKey.stringKey("log.level"), "INFO")
                        .emit()

                    val validateSpan = tracer.spanBuilder("validate-order").startSpan()
                    validateSpan.makeCurrent().use {
                        validateSpan.setAttribute("validation.rules", 3)
                        validateSpan.addEvent("validation-passed")
                    }
                    validateSpan.end()

                    val chargeSpan = tracer.spanBuilder("charge-payment").startSpan()
                    chargeSpan.makeCurrent().use {
                        chargeSpan.setAttribute("payment.method", "card")
                        chargeSpan.setAttribute("payment.amount", 42.50)
                        chargeSpan.addEvent("payment-authorised")

                        logger.logRecordBuilder()
                            .setBody("Payment charged for order-456")
                            .setAttribute(AttributeKey.stringKey("log.level"), "INFO")
                            .emit()
                    }
                    chargeSpan.end()

                    events(RequestProcessed("/orders", "user-99"))

                    Span.current().setStatus(StatusCode.OK)
                    Span.current().addEvent("order-completed")
                }
        }
        parentSpan.end()

        val traces = intercept.traceStore.traces(Descending)
        assertThat(traces.size, greaterThan(0))

        val spans = traces.values.first()
        assertThat(spans.size, greaterThan(2))
    }

    @Test
    fun `second test also works with fresh global registration`(http: HttpHandler) {
        val app = App(http)
        app(Request(GET, "/other"))

        val traces = intercept.traceStore.traces(Descending)
        assertThat(traces.size, greaterThan(0))
    }
}
