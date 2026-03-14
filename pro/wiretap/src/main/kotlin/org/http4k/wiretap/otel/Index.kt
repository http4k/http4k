/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.otel

import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.lens.Query
import org.http4k.lens.html
import org.http4k.lens.value
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.template.TemplateRenderer
import org.http4k.template.ViewModel
import org.http4k.wiretap.domain.OtelTraceId
import org.http4k.wiretap.util.Json
import org.http4k.wiretap.util.SignalModel

fun Index(templates: TemplateRenderer): RoutingHttpHandler =
    "/" bind GET to { req ->
        val trace = traceParam(req)
        Response(OK).html(templates(Index(trace)))
    }

private val traceParam = Query.value(OtelTraceId).optional("trace")

data class OtelSignals(
    val selectedTrace: OtelTraceId? = null
) : SignalModel

data class Index(val deepLinkTrace: OtelTraceId? = null) : ViewModel {
    val initialSignals = Json.asFormatString(OtelSignals(selectedTrace = deepLinkTrace))
}
