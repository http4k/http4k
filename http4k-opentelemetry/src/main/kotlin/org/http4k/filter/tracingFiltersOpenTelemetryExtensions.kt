package org.http4k.filter

import io.grpc.Context
import io.opentelemetry.OpenTelemetry.getPropagators
import io.opentelemetry.context.propagation.TextMapPropagator.Getter
import io.opentelemetry.context.propagation.TextMapPropagator.Setter
import io.opentelemetry.trace.Span.Kind.CLIENT
import io.opentelemetry.trace.Span.Kind.SERVER
import io.opentelemetry.trace.StatusCanonicalCode.ERROR
import io.opentelemetry.trace.Tracer
import io.opentelemetry.trace.TracingContextUtils.currentContextWith
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

    val textMapPropagator = getPropagators().textMapPropagator
    val setter = setter<Request>()

    return Filter { next ->
        { req ->
            with(tracer.spanBuilder(spanNamer(req)).setSpanKind(CLIENT).startSpan()) {
                setAttribute("http.method", req.method.name)
                setAttribute("http.url", req.uri.toString())
                try {
                    currentContextWith(this).use {
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
    val textMapPropagator = getPropagators().textMapPropagator

    val getter = getter<Request>()
    val setter = setter<Response>()
    return Filter { next ->
        { req ->
            with(tracer.spanBuilder(spanNamer(req))
                .setParent(textMapPropagator.extract(Context.current(), req, getter))
                .setSpanKind(SERVER)
                .startSpan()
            ) {
                setAttribute("http.method", req.method.name)
                setAttribute("http.url", req.uri.toString())
                if (req is RoutedRequest) setAttribute("http.route", req.xUriTemplate.toString())

                currentContextWith(this).use {
                    try {
                        val ref = AtomicReference(next(req))
                        textMapPropagator.inject(Context.current(), ref, setter)
                        ref.get()
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

internal fun <T : HttpMessage> getter() = Getter<T> { req, name -> req.header(name) }
