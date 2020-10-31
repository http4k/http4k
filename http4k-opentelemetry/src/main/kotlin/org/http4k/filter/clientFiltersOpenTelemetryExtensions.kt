package org.http4k.filter

import io.grpc.Context
import io.opentelemetry.OpenTelemetry
import io.opentelemetry.context.propagation.TextMapPropagator
import io.opentelemetry.trace.Span
import io.opentelemetry.trace.StatusCanonicalCode
import io.opentelemetry.trace.Tracer
import io.opentelemetry.trace.TracingContextUtils
import org.http4k.core.Filter
import org.http4k.core.Request
import java.util.concurrent.atomic.AtomicReference

fun ClientFilters.OpenTelemetryTracing(tracer: Tracer,
                                       spanNamer: (Request) -> String = { it.uri.toString() },
                                       error: (Request, Throwable) -> String = { _, t -> t.localizedMessage }
): Filter {

    val textMapPropagator = OpenTelemetry.getPropagators().textMapPropagator

    val setter = TextMapPropagator.Setter<AtomicReference<Request>> { ref, name, value ->
        ref?.run { set(get().header(name, value)) }
    }

    return Filter { next ->
        { req ->
            with(tracer.spanBuilder(spanNamer(req)).setSpanKind(Span.Kind.CLIENT).startSpan()) {
                setAttribute("http.method", req.method.name);
                setAttribute("http.url", req.uri.toString());
                try {
                    TracingContextUtils.currentContextWith(this).use {
                        val ref = AtomicReference(req)
                        textMapPropagator.inject(Context.current(), ref, setter)
                        next(ref.get()).apply {
                            setAttribute("http.status_code", status.code.toString());
                        }
                    }
                } catch (t: Throwable) {
                    setStatus(StatusCanonicalCode.ERROR, error(req, t))
                    throw t
                } finally {
                    end()
                }
            }
        }
    }
}
