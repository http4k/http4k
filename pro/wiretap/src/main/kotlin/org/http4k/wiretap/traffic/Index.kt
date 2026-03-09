/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.traffic

import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.lens.Query
import org.http4k.lens.html
import org.http4k.lens.string
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.template.TemplateRenderer
import org.http4k.template.ViewModel
import org.http4k.wiretap.util.Json
import org.http4k.wiretap.util.SignalModel

fun Index(templates: TemplateRenderer, findByTraceId: (String) -> Long?): RoutingHttpHandler =
    "/" bind GET to { req ->
        Response(OK).html(templates(Index(Query.string().optional("trace")(req)?.let { findByTraceId(it) })))
    }

data class TrafficSignals(
    val direction: String? = null,
    val host: String? = null,
    val method: String? = null,
    val status: String? = null,
    val path: String? = null,
    val activeView: String = "all",
    val customView: Boolean = false,
    val showAddView: Boolean = false,
    val name: String? = null,
    val selectedTx: Long? = null
) : SignalModel

data class Index(val deepLinkTxId: Long? = null) : ViewModel {
    val initialSignals = Json.asFormatString(TrafficSignals(selectedTx = deepLinkTxId))
}
