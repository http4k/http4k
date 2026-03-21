/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.traffic

import org.http4k.ai.mcp.server.capability.CapabilityPack
import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.datastar.MorphMode
import org.http4k.datastar.Selector
import org.http4k.lens.Path
import org.http4k.lens.datastarElements
import org.http4k.lens.long
import org.http4k.routing.bind
import org.http4k.template.DatastarElementRenderer
import org.http4k.template.TemplateRenderer
import org.http4k.wiretap.WiretapFunction
import org.http4k.wiretap.domain.Ordering.Descending
import org.http4k.wiretap.domain.TransactionFilter
import org.http4k.wiretap.domain.TransactionStore
import org.http4k.wiretap.domain.ViewStore
import org.http4k.wiretap.domain.toSummary
import org.http4k.wiretap.util.datastarSignal
import java.time.Clock

fun ActivateView(viewStore: ViewStore, transactionStore: TransactionStore, clock: Clock) = object : WiretapFunction {
    override fun http(elements: DatastarElementRenderer, html: TemplateRenderer) =
        "/views/{id}/activate" bind POST to { req ->
            val id = Path.long().of("id")(req)
            val view = viewStore.list().find { it.id == id }
            val signals = view?.let { ViewActivationSignals(it) } ?: ViewActivationSignals.Reset
            val filter = view?.filter ?: TransactionFilter()
            val rows = transactionStore.list(Descending, filter, Int.MAX_VALUE).map { it.toSummary(clock) }.map { TransactionRowView(it) }

            Response(OK)
                .datastarSignal(signals)
                .datastarElements(
                    rows.flatMap { elements(it) },
                    MorphMode.inner,
                    Selector.of("#tx-list")
                )
        }

    override fun mcp() = CapabilityPack()
}
