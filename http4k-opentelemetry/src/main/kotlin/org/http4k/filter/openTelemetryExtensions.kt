package org.http4k.filter

import io.grpc.Context.current
import io.opentelemetry.OpenTelemetry
import io.opentelemetry.context.ContextUtils.withScopedContext
import io.opentelemetry.context.propagation.TextMapPropagator
import io.opentelemetry.context.propagation.TextMapPropagator.Setter
import io.opentelemetry.trace.Span.Kind.CLIENT
import io.opentelemetry.trace.Span.Kind.SERVER
import io.opentelemetry.trace.StatusCanonicalCode.ERROR
import io.opentelemetry.trace.Tracer
import io.opentelemetry.trace.TracingContextUtils.currentContextWith
import org.http4k.core.Filter
import org.http4k.core.Request
import org.http4k.routing.RoutedRequest
import java.util.concurrent.atomic.AtomicReference

fun ServerFilters.OpenTelemetryTracing(tracer: Tracer,
                                       error: (Request, Throwable) -> String = { _, t -> t.localizedMessage }
): Filter {
    val textMapPropagator = OpenTelemetry.getPropagators().textMapPropagator

    val getter = TextMapPropagator.Getter<Request> { carrier, key ->
        carrier.header(key)
    }

    return Filter { next ->
        { req ->
            val context = current()
            withScopedContext(textMapPropagator.extract(context, req, getter)).use {
                with(tracer.spanBuilder(req.uri.toString()).setParent(context).setSpanKind(SERVER).startSpan()) {
                    try {
                        setAttribute("http.method", req.method.name)
                        setAttribute("http.url", req.uri.toString())
                        if (req is RoutedRequest) setAttribute("http.route", req.xUriTemplate.toString())
                        next(req)
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

fun ClientFilters.OpenTelemetryTracing(tracer: Tracer,
                                       error: (Request, Throwable) -> String = { _, t -> t.localizedMessage }
): Filter {

    val textMapPropagator = OpenTelemetry.getPropagators().textMapPropagator

    val setter = Setter<AtomicReference<Request>> { ref, key, value ->
        ref?.run { set(get().header(key, value)) }
    }

    return Filter { next ->
        { req ->
            with(tracer.spanBuilder(req.uri.toString()).setSpanKind(CLIENT).startSpan()) {
                try {
                    val current = current()
                    currentContextWith(this).use {
                        setAttribute("http.method", req.method.name);
                        setAttribute("http.url", req.uri.toString());
                        val ref = AtomicReference(req)
                        textMapPropagator.inject(current, ref, setter)
                        next(ref.get()).apply {
                            setAttribute("http.status_code", status.code.toString());
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
