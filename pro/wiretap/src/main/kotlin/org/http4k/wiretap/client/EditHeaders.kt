/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.client

import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.datastar.MorphMode.inner
import org.http4k.datastar.Selector
import org.http4k.lens.Path
import org.http4k.lens.datastarElements
import org.http4k.lens.int
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.template.DatastarElementRenderer
import org.http4k.template.ViewModel
import org.http4k.wiretap.util.SignalModel
import org.http4k.wiretap.util.datastarSignal

data class HeaderRowView(val key: String, val index: Int, val basePath: String)

data class HeaderRowsView(
    val rows: List<HeaderRowView>,
    val showAdd: Boolean,
    val basePath: String
) : ViewModel

fun headerRowsView(headers: Map<String, HeaderEntry>, basePath: String) =
    HeaderRowsView(
        rows = headers.entries.toList().mapIndexed { index, (key, _) -> HeaderRowView(key, index, basePath) },
        showAdd = headers.size < 10,
        basePath = basePath
    )

data class HeadersSignals(val headers: Map<String, HeaderEntry>) : SignalModel

fun EditHeaders(
    elements: DatastarElementRenderer,
    basePath: String = "/_wiretap/inbound/",
    defaultUrl: String = "/"
): RoutingHttpHandler {
    fun headerResponse(headers: Map<String, HeaderEntry>, signals: SignalModel) =
        Response(OK)
            .datastarSignal(signals)
            .datastarElements(
                elements(headerRowsView(headers, basePath)),
                morphMode = inner,
                selector = Selector.of("#client-headers")
            )

    return "headers" bind routes(
        "/reset" bind POST to {
            headerResponse(ClientSignals(url = defaultUrl).headers, ClientSignals(url = defaultUrl))
        },
        "/add" bind POST to { req ->
            val model = clientRequestLens(req)
            val nextIndex = (model.headers.keys.mapNotNull { it.toIntOrNull() }.maxOrNull() ?: -1) + 1
            val newHeaders = model.headers + (nextIndex.toString() to HeaderEntry())
            headerResponse(newHeaders, HeadersSignals(newHeaders))
        },
        "/remove/{n}" bind POST to { req ->
            val n = Path.int().of("n")(req)
            val model = clientRequestLens(req)
            if (model.headers.size <= 1) return@to Response(OK)

            val remaining = model.headers.entries.toList()
                .filterIndexed { index, _ -> index != n }
                .associate { (key, entry) -> key to entry }

            headerResponse(remaining, HeadersSignals(remaining))
        }
    )
}
