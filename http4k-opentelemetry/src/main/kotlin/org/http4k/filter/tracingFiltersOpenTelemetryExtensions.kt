package org.http4k.filter

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanBuilder
import io.opentelemetry.api.trace.SpanKind.CLIENT
import io.opentelemetry.api.trace.SpanKind.SERVER
import io.opentelemetry.api.trace.StatusCode.ERROR
import io.opentelemetry.context.Context
import io.opentelemetry.context.propagation.TextMapGetter
import io.opentelemetry.context.propagation.TextMapPropagator
import io.opentelemetry.context.propagation.TextMapSetter
import org.http4k.core.Filter
import org.http4k.core.HttpMessage
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.metrics.Http4kOpenTelemetry.INSTRUMENTATION_NAME
import org.http4k.routing.RoutedRequest
import java.util.concurrent.atomic.AtomicReference

fun ClientFilters.OpenTelemetryTracing(
    openTelemetry: OpenTelemetry = GlobalOpenTelemetry.get(),
    spanNamer: (Request) -> String = { it.uri.toString() },
    error: (Request, Throwable) -> String = { _, t -> t.localizedMessage },
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
                        next(ref.get()).apply {
                            setAttribute("http.status_code", status.code.toString())
                            spanCompletionMutator(this@with, req, this)
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
    spanNamer: (Request) -> String = { it.uri.toString() },
    error: (Request, Throwable) -> String = { _, t -> t.localizedMessage },
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
                            spanCompletionMutator(this, req, it)
                            setAttribute("http.status_code", it.status.code.toString())
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
