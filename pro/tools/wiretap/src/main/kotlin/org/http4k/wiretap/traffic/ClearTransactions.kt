package org.http4k.wiretap.traffic

import org.http4k.ai.mcp.ToolResponse.Ok
import org.http4k.ai.mcp.model.Content.Text
import org.http4k.ai.mcp.model.Tool
import org.http4k.core.Method.DELETE
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NO_CONTENT
import org.http4k.routing.bind
import org.http4k.template.DatastarElementRenderer
import org.http4k.wiretap.WiretapFunction
import org.http4k.wiretap.domain.TransactionStore

fun ClearTransaction(transactionStore: TransactionStore) = object : WiretapFunction {
    override fun http(renderer: DatastarElementRenderer) = "/" bind DELETE to {
        transactionStore.clear()
        Response(NO_CONTENT)
    }

    override fun mcp() = Tool(
        "clear_transactions",
        "Clear all recorded HTTP transactions"
    ) bind {
        transactionStore.clear()
        Ok(listOf(Text("All transactions cleared")))
    }
}
