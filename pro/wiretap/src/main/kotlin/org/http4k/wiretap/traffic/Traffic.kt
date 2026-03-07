package org.http4k.wiretap.traffic

import org.http4k.ai.mcp.server.capability.CapabilityPack
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.template.DatastarElementRenderer
import org.http4k.template.TemplateRenderer
import org.http4k.wiretap.WiretapFunction
import org.http4k.wiretap.domain.TransactionStore
import org.http4k.wiretap.domain.ViewStore
import org.http4k.wiretap.domain.traceparent
import java.time.Clock

fun Traffic(transactionStore: TransactionStore, viewStore: ViewStore, clock: Clock) = object : WiretapFunction {
    private val functions = listOf(
        ListTransactions(transactionStore, clock),
        ClearTransaction(transactionStore),
        ListViews(viewStore),
        CreateView(viewStore),
        UpdateView(viewStore),
        DeleteView(viewStore),
        ActivateView(viewStore, transactionStore, clock),
        ExportHar(transactionStore),
        ViewTransaction(transactionStore, clock),
    )

    override fun http(elements: DatastarElementRenderer, html: TemplateRenderer) =
        "traffic" bind routes(
            functions.map { it.http(elements, html) } + Index(html) { traceId ->
                transactionStore.list().find { it.traceparent() == traceId }?.id
            }
        )

    override fun mcp() = CapabilityPack(functions.map { it.mcp() })
}
