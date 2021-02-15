package org.http4k.filter

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.Span.Kind.CLIENT
import io.opentelemetry.api.trace.Span.Kind.SERVER
import io.opentelemetry.api.trace.StatusCode.ERROR
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.context.Context
import io.opentelemetry.context.propagation.TextMapPropagator
import io.opentelemetry.context.propagation.TextMapPropagator.Getter
import io.opentelemetry.context.propagation.TextMapPropagator.Setter
import org.http4k.core.Filter
import org.http4k.core.HttpMessage
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.metrics.Http4kOpenTelemetry
import org.http4k.routing.RoutedRequest
import java.util.concurrent.atomic.AtomicReference

fun ClientFilters.OpenTelemetryTracing(tracer: Tracer = Http4kOpenTelemetry.tracer,
                                       spanNamer: (Request) -> String = { it.uri.toString() },
                                       error: (Request, Throwable) -> String = { _, t -> t.localizedMessage }
): Filter {

    val textMapPropagator = OpenTelemetry.getGlobalPropagators().textMapPropagator
    val setter = setter<Request>()

    return Filter { next ->
        { req ->
            with(tracer.spanBuilder(spanNamer(req)).setSpanKind(CLIENT).startSpan()) {
                try {
                    setAttribute("http.method", req.method.name)
                    setAttribute("http.url", req.uri.toString())
                    makeCurrent().use {
                        val ref = AtomicReference(req)
                        textMapPropagator.inject(Context.current(), ref, setter)
                        next(ref.get()).apply {
                            setAttribute("http.status_code", status.code.toString())
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

fun ServerFilters.OpenTelemetryTracing(tracer: Tracer = Http4kOpenTelemetry.tracer,
                                       spanNamer: (Request) -> String = { it.uri.toString() },
                                       error: (Request, Throwable) -> String = { _, t -> t.localizedMessage }
): Filter {
    val textMapPropagator = OpenTelemetry.getGlobalPropagators().textMapPropagator

    val getter = getter<Request>(textMapPropagator)
    val setter = setter<Response>()
    return Filter { next ->
        { req ->
            with(tracer.spanBuilder(spanNamer(req))
                .setParent(textMapPropagator.extract(Context.current(), req, getter))
                .setSpanKind(SERVER)
                .startSpan()) {
                makeCurrent().use {
                    try {
                        if (req is RoutedRequest) setAttribute("http.route", req.xUriTemplate.toString())
                        setAttribute("http.method", req.method.name)
                        setAttribute("http.url", req.uri.toString())
                        val ref = AtomicReference(next(req))
                        textMapPropagator.inject(Context.current(), ref, setter)
                        ref.get().also { setAttribute("http.status_code", it.status.code.toString()) }
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
internal fun <T : HttpMessage> setter() = Setter<AtomicReference<T>> { ref, name, value ->
    ref?.run { set(get().header(name, value) as T) }
}

internal fun <T : HttpMessage> getter(textMapPropagator: TextMapPropagator) = object : Getter<T> {
    override fun keys(carrier: T) = textMapPropagator.fields()

    override fun get(carrier: T?, key: String) = carrier?.header(key)
}
