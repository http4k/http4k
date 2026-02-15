package org.http4k.filter

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanBuilder
import io.opentelemetry.api.trace.SpanKind.SERVER
import io.opentelemetry.api.trace.StatusCode.ERROR
import io.opentelemetry.api.trace.StatusCode.UNSET
import io.opentelemetry.context.Context
import io.opentelemetry.context.propagation.TextMapGetter
import io.opentelemetry.context.propagation.TextMapPropagator
import io.opentelemetry.context.propagation.TextMapSetter
import org.http4k.core.HttpMessage
import org.http4k.core.Request
import org.http4k.metrics.Http4kOpenTelemetry.INSTRUMENTATION_NAME
import org.http4k.routing.RequestWithContext
import org.http4k.routing.RoutedMessage
import java.util.concurrent.atomic.AtomicReference


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
    private val spanCreationMutator: (SpanBuilder, Request) -> SpanBuilder,
    private val attributesKeys: OpenTelemetryAttributesKeys
) {
    private val tracer = openTelemetry.tracerProvider.get(INSTRUMENTATION_NAME)
    private val textMapPropagator = openTelemetry.propagators.textMapPropagator
    internal val getter = getter<Request>(textMapPropagator)

    fun createServerSpan(req: Request): Span =
        tracer.spanBuilder(spanNamer(req))
            .setParent(textMapPropagator.extract(Context.current(), req, getter))
            .setSpanKind(SERVER)
            .setServerSpanAttributes(req, attributesKeys)
            .let { spanCreationMutator(it, req) }
            .startSpan()

    fun setSpanError(span: Span, req: Request, t: Throwable) {
        span.setStatus(ERROR, error(req, t))
    }
}

internal fun SpanBuilder.setServerSpanAttributes(
    req: Request,
    attributeKeys: OpenTelemetryAttributesKeys
): SpanBuilder = apply {
    if (req is RoutedMessage && req.xUriTemplate != null) {
        setAttribute(attributeKeys.httpRoute, req.xUriTemplate.toString())
    }
    setAttribute(attributeKeys.method, req.method.name)
    attributeKeys.serverUrl?.let { setAttribute(it, req.uri.toString()) }
    req.header("User-Agent")?.also { setAttribute(attributeKeys.userAgent, it) }
    req.remoteAddress()?.also { setAttribute(attributeKeys.clientAddress, it) }
}

