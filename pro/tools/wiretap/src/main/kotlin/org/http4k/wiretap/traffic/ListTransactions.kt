package org.http4k.wiretap.traffic

import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.model.enum
import org.http4k.ai.mcp.model.int
import org.http4k.ai.mcp.model.status
import org.http4k.ai.mcp.model.string
import org.http4k.ai.mcp.server.capability.ToolCapability
import org.http4k.core.Method
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.datastar.MorphMode
import org.http4k.datastar.Selector
import org.http4k.lens.datastarElements
import org.http4k.routing.bind
import org.http4k.template.DatastarElementRenderer
import org.http4k.template.TemplateRenderer
import org.http4k.template.ViewModel
import org.http4k.wiretap.WiretapFunction
import org.http4k.wiretap.domain.Direction
import org.http4k.wiretap.domain.Direction.Inbound
import org.http4k.wiretap.domain.TransactionFilter
import org.http4k.wiretap.domain.TransactionStore
import org.http4k.wiretap.domain.TransactionSummary
import org.http4k.wiretap.domain.toSummary
import org.http4k.wiretap.util.Json
import org.http4k.wiretap.util.Json.datastarModel

fun ListTransactions(transactionStore: TransactionStore) = object : WiretapFunction {
    private fun list(filter: TransactionFilter, limit: Int) =
        transactionStore.list(filter, limit).map { it.toSummary() }

    override fun http(elements: DatastarElementRenderer, html: TemplateRenderer) =
        "/list" bind GET to { req ->
            val filter = req.datastarModel<TransactionFilterSignals>().toFilter()
            val rows = list(filter, 50).map { TransactionRowView(it) }

            Response(OK).datastarElements(
                rows.flatMap { elements(it) },
                MorphMode.inner,
                Selector.of("#tx-list")
            )
        }

    override fun mcp(): ToolCapability {
        val direction = Tool.Arg.enum<Direction>().optional("direction", "Filter by direction: Inbound or Outbound")
        val method = Tool.Arg.enum<Method>().optional("method", "Filter by HTTP method (GET, POST, etc)")
        val status = Tool.Arg.status().optional("status", "Filter by status code regex (e.g. '404', '4..', '2\\d\\d')")
        val path = Tool.Arg.string().optional("path", "Filter by path substring (case-insensitive)")
        val host = Tool.Arg.string().optional("host", "Filter by host substring (case-insensitive)")
        val limit = Tool.Arg.int().optional("limit", "Maximum number of transactions to return (default 50)")

        return Tool(
            "list_transactions",
            "List recent HTTP transactions captured by Wiretap with optional filtering",
            direction, method, status, path, host, limit
        ) bind {
            val filter = TransactionFilter(direction(it), host(it), method(it), status(it), path(it))
            Json.asToolResponse(list(filter, limit(it) ?: 50))
        }
    }
}

data class TransactionRowView(val tx: TransactionSummary) : ViewModel {
    val isInbound = tx.direction == Inbound
    val dirArrow = if (isInbound) "\u2193" else "\u2191"
    val dirClass = if (isInbound) "dir-in" else "dir-out"
    val badgeClass = if (isInbound) "badge-in" else "badge-out"
    val badgeText = if (isInbound) "IN" else "OUT"
    val statusClass = statusClass(tx.status)
}
