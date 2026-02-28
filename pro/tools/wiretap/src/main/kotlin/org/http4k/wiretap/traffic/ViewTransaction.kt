package org.http4k.wiretap.traffic

import org.http4k.ai.mcp.ToolResponse
import org.http4k.ai.mcp.model.Content
import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.model.long
import org.http4k.ai.mcp.server.capability.ToolCapability
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.datastar.Selector
import org.http4k.lens.Path
import org.http4k.lens.datastarElements
import org.http4k.lens.long
import org.http4k.routing.bind
import org.http4k.template.DatastarElementRenderer
import org.http4k.template.TemplateRenderer
import org.http4k.template.ViewModel
import org.http4k.wiretap.WiretapFunction
import org.http4k.wiretap.domain.TransactionDetail
import org.http4k.wiretap.domain.TransactionStore
import org.http4k.wiretap.domain.toDetail
import org.http4k.wiretap.util.Json

fun GetTransaction(transactionStore: TransactionStore) = object : WiretapFunction {
    private fun lookup(id: Long) = transactionStore.get(id)?.toDetail()

    override fun http(elements: DatastarElementRenderer, html: TemplateRenderer) =
        "/{id}" bind GET to { req ->
            val id = Path.long().of("id")(req)
            when (val detail = lookup(id)?.let { TransactionDetailView(it) }) {
                null -> Response(NOT_FOUND)
                else -> Response(OK).datastarElements(
                    elements(detail),
                    selector = Selector.of("#detail-panel")
                )
            }
        }

    override fun mcp(): ToolCapability {
        val id = Tool.Arg.long().required("id", "Transaction ID")

        return Tool(
            "get_transaction",
            "Get full request/response detail for a specific HTTP transaction",
            id
        ) bind { req ->
            when (val tx = lookup(id(req))) {
                null -> ToolResponse.Error("Transaction not found")
                else -> ToolResponse.Ok(listOf(Content.Text(Json.asFormatString(tx))))
            }
        }
    }
}

data class TransactionDetailView(val tx: TransactionDetail, val showImport: Boolean = true) : ViewModel {
    val isInbound = tx.direction == "Inbound"
    val dirBadgeClass = if (isInbound) "badge-in" else "badge-out"
    val dirBadgeText = if (isInbound) "INBOUND" else "OUTBOUND"
    val importPath = if (isInbound) "/_wiretap/client" else "/_wiretap/outbound"
    val importLabel = if (isInbound) "Inbound Client" else "Outbound Client"
    val statusClass = statusClass(tx.status)
    val shortTraceId = tx.traceId?.takeLast(8)
}
