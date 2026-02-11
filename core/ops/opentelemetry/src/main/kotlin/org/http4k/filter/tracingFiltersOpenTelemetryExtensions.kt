package org.http4k.filter

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanBuilder
import io.opentelemetry.api.trace.SpanKind.CLIENT
import io.opentelemetry.api.trace.SpanKind.SERVER
import io.opentelemetry.api.trace.StatusCode.ERROR
import io.opentelemetry.api.trace.StatusCode.UNSET
import io.opentelemetry.context.Context
import io.opentelemetry.context.propagation.TextMapGetter
import io.opentelemetry.context.propagation.TextMapPropagator
import io.opentelemetry.context.propagation.TextMapSetter
import io.opentelemetry.semconv.ServerAttributes.SERVER_ADDRESS
import io.opentelemetry.semconv.ServerAttributes.SERVER_PORT
import io.opentelemetry.semconv.incubating.HttpIncubatingAttributes.HTTP_REQUEST_BODY_SIZE
import io.opentelemetry.semconv.incubating.HttpIncubatingAttributes.HTTP_RESPONSE_BODY_SIZE
import org.http4k.core.Filter
import org.http4k.core.HttpMessage
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.metrics.Http4kOpenTelemetry.INSTRUMENTATION_NAME
import org.http4k.routing.RequestWithContext
import org.http4k.routing.RoutedMessage
import java.util.concurrent.atomic.AtomicReference

fun ClientFilters.OpenTelemetryTracing(
    openTelemetry: OpenTelemetry = GlobalOpenTelemetry.get(),
    spanNamer: (Request) -> String = defaultSpanNamer,
    error: (Request, Throwable) -> String = { _, t -> t.message ?: "no message" },
    spanCreationMutator: (SpanBuilder) -> SpanBuilder = { it },
    spanCompletionMutator: (Span, Request, Response) -> Unit = { _, _, _ -> },
): Filter {
    val tracer = openTelemetry.tracerProvider.get(INSTRUMENTATION_NAME)
    val textMapPropagator = openTelemetry.propagators.textMapPropagator
    val setter = setter<Request>()

    return Filter { next ->
        { req ->
            with(
                tracer.spanBuilder(spanNamer(req))
                .setSpanKind(CLIENT)
                .apply {
                    setAttribute("http.method", req.method.name)
                    setAttribute("http.url", req.uri.toString())
                }
                .let { spanCreationMutator(it) }
                .startSpan()) {
                try {
                    makeCurrent().use {
                        val ref = AtomicReference(req)
                        textMapPropagator.inject(Context.current(), ref, setter)
                        next(ref.get()).also {
                            setAttribute(SERVER_ADDRESS, req.uri.host)
                            req.uri.port?.also { if (it != 80 && it != 443) setAttribute(SERVER_PORT, it) }

                            addStandardDataFrom(it, req)

                            spanCompletionMutator(this@with, req, it)
                            if (it.status.clientError || it.status.serverError) setStatus(ERROR)
                        }
                    }
                } catch (t: Throwable) {
                    setStatus(ERROR, error(req, t))
                    throw t
                } finally {
                    end()
                }
            }
        }
    }
}

fun ServerFilters.OpenTelemetryTracing(
    openTelemetry: OpenTelemetry = GlobalOpenTelemetry.get(),
    spanNamer: (Request) -> String = defaultSpanNamer,
    error: (Request, Throwable) -> String = { _, t -> t.message ?: "no message" },
    spanCreationMutator: (SpanBuilder, Request) -> SpanBuilder = { spanBuilder, _ -> spanBuilder },
    spanCompletionMutator: (Span, Request, Response) -> Unit = { _, _, _ -> },
): Filter {
    val context = ServerTracingContext(openTelemetry, spanNamer, error, spanCreationMutator)
    val textMapPropagator = openTelemetry.propagators.textMapPropagator
    val setter = setter<Response>()

    return Filter { next ->
        { req ->
            with(context.createServerSpan(req)) {
                makeCurrent().use {
                    try {
                        val ref = AtomicReference(next(req))

                        textMapPropagator.inject(Context.current(), ref, setter)
                        ref.get().also {
                            addStandardDataFrom(it, req)
                            spanCompletionMutator(this, req, it)
                            setStatusFromResponse(it.status)
                        }
                    } catch (t: Throwable) {
                        context.setSpanError(this, req, t)
                        throw t
                    } finally {
                        end()
                    }
                }
            }
        }
    }
}

@Suppress("UNCHECKED_CAST")
internal fun <T : HttpMessage> setter() = TextMapSetter<AtomicReference<T>> { ref, name, value ->
    ref?.run { set(get().header(name, value) as T) }
}

internal fun <T : HttpMessage> getter(textMapPropagator: TextMapPropagator) = object : TextMapGetter<T> {
    override fun keys(carrier: T) = textMapPropagator.fields()

    override fun get(carrier: T?, key: String) = carrier?.header(key)
}

val defaultSpanNamer: (Request) -> String = {
    when (it) {
        is RequestWithContext -> it.method.name + it.xUriTemplate?.let { " $it" }
        else -> it.method.name
    }
}

private fun Span.addStandardDataFrom(resp: Response, req: Request) {
    resp.body.length?.also {
        setAttribute(HTTP_RESPONSE_BODY_SIZE, it)
        setAttribute("message.type", "RECEIVED")
        setAttribute("messaging.message_payload_size_bytes", it)
    }
    req.body.length?.also { setAttribute(HTTP_REQUEST_BODY_SIZE, it) }
    setAttribute("http.status_code", resp.status.code.toLong())
}

internal fun Request.remoteAddress(): String? =
    header("X-Forwarded-For")?.split(",")?.firstOrNull() ?: source?.address

internal fun Span.setStatusFromResponse(status: org.http4k.core.Status) {
    if (status.serverError) setStatus(ERROR)
    else if (status.clientError) setStatus(UNSET)
}

internal class ServerTracingContext(
    openTelemetry: OpenTelemetry,
    private val spanNamer: (Request) -> String,
    private val error: (Request, Throwable) -> String,
    private val spanCreationMutator: (SpanBuilder, Request) -> SpanBuilder
) {
    private val tracer = openTelemetry.tracerProvider.get(INSTRUMENTATION_NAME)
    private val textMapPropagator = openTelemetry.propagators.textMapPropagator
    internal val getter = getter<Request>(textMapPropagator)

    fun createServerSpan(req: Request): Span =
        tracer.spanBuilder(spanNamer(req))
            .setParent(textMapPropagator.extract(Context.current(), req, getter))
            .setSpanKind(SERVER)
            .setServerSpanAttributes(req)
            .let { spanCreationMutator(it, req) }
            .startSpan()

    fun setSpanError(span: Span, req: Request, t: Throwable) {
        span.setStatus(ERROR, error(req, t))
    }
}

internal fun SpanBuilder.setServerSpanAttributes(req: Request): SpanBuilder = apply {
    if (req is RoutedMessage && req.xUriTemplate != null) {
        setAttribute("http.route", req.xUriTemplate.toString())
    }
    setAttribute("http.method", req.method.name)
    setAttribute("http.url", req.uri.toString())
    req.header("User-Agent")?.also { setAttribute("http.user_agent", it) }
    req.remoteAddress()?.also { setAttribute("http.client_ip", it) }
}

