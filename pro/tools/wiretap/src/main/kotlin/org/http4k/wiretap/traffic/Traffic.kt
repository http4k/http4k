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

fun Traffic(
    transactionStore: TransactionStore,
    viewStore: ViewStore,
    templates: TemplateRenderer,
) = object : WiretapFunction {
    private val functions = listOf(
        ListTransactions(transactionStore),
        ClearTransaction(transactionStore),
        ListViews(viewStore::list),
        CreateView(viewStore),
        UpdateView(viewStore::update, viewStore::list),
        DeleteView(viewStore::remove, viewStore::list),
        GetTransaction(transactionStore),
    )

    override fun http(renderer: DatastarElementRenderer) =
        "traffic" bind routes(
            functions.map { it.http(renderer) } + Index(templates) { traceId ->
                transactionStore.list().find { it.traceparent() == traceId }?.id
            }
        )

    override fun mcp() = CapabilityPack(functions.map { it.mcp() })
}
