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
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes.*
import org.http4k.core.Filter
import org.http4k.core.HttpMessage
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.metrics.Http4kOpenTelemetry.INSTRUMENTATION_NAME
import org.http4k.routing.RoutedRequest
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
            with(tracer.spanBuilder(spanNamer(req))
                .setSpanKind(CLIENT)
                .let { spanCreationMutator(it) }
                .startSpan()) {
                try {
                    setAttribute("http.method", req.method.name)
                    setAttribute("http.url", req.uri.toString())
                    makeCurrent().use {
                        val ref = AtomicReference(req)
                        textMapPropagator.inject(Context.current(), ref, setter)
                        next(ref.get()).also {
                            setAttribute(NET_PEER_NAME, req.uri.host)
                            req.uri.port?.also { if (it != 80 && it != 443) setAttribute(NET_PEER_PORT, it) }

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
    val tracer = openTelemetry.tracerProvider.get(INSTRUMENTATION_NAME)
    val textMapPropagator = openTelemetry.propagators.textMapPropagator
    val getter = getter<Request>(textMapPropagator)
    val setter = setter<Response>()

    return Filter { next ->
        { req ->
            with(tracer.spanBuilder(spanNamer(req))
                .setParent(textMapPropagator.extract(Context.current(), req, getter))
                .setSpanKind(SERVER)
                .let { spanCreationMutator(it, req) }
                .startSpan()) {
                makeCurrent().use {
                    try {
                        if (req is RoutedRequest) setAttribute("http.route", req.xUriTemplate.toString())
                        setAttribute("http.method", req.method.name)
                        setAttribute("http.url", req.uri.toString())
                        val ref = AtomicReference(next(req))

                        textMapPropagator.inject(Context.current(), ref, setter)
                        ref.get().also {
                            addStandardDataFrom(it, req)
                            spanCompletionMutator(this, req, it)
                            if (it.status.serverError) setStatus(ERROR)
                            else if (it.status.clientError) setStatus(UNSET)
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
        is RoutedRequest -> it.method.name + it.xUriTemplate
        else -> it.method.name
    }
}

private fun Span.addStandardDataFrom(resp: Response, req: Request) {
    resp.body.length?.also { setAttribute(HTTP_RESPONSE_CONTENT_LENGTH, it) }
    req.body.length?.also { setAttribute(HTTP_REQUEST_CONTENT_LENGTH, it) }
    setAttribute("http.status_code", resp.status.code.toString())
}

