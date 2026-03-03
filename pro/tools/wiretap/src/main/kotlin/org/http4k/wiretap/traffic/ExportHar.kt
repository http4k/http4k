package org.http4k.wiretap.traffic

import org.http4k.ai.mcp.ToolResponse.Error
import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.model.long
import org.http4k.ai.mcp.server.capability.ToolCapability
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.lens.Header.CONTENT_TYPE
import org.http4k.lens.Path
import org.http4k.lens.long
import org.http4k.routing.bind
import org.http4k.template.DatastarElementRenderer
import org.http4k.template.TemplateRenderer
import org.http4k.wiretap.WiretapFunction
import org.http4k.wiretap.domain.TransactionStore
import org.http4k.wiretap.domain.toHar
import org.http4k.wiretap.util.Json
import org.http4k.wiretap.util.Json.json

fun ExportHar(transactionStore: TransactionStore) = object : WiretapFunction {
    override fun http(elements: DatastarElementRenderer, html: TemplateRenderer) =
        "/{id}/har" bind GET to { req ->
            val id = Path.long().of("id")(req)
            when (val har = transactionStore.get(id)?.toHar()) {
                null -> Response(NOT_FOUND)
                else -> Response(OK)
                    .with(CONTENT_TYPE of APPLICATION_JSON)
                    .header("Content-Disposition", """attachment; filename="transaction-$id.har"""")
                    .json(har)
            }
        }

    override fun mcp(): ToolCapability {
        val id = Tool.Arg.long().required("id", "Transaction ID")

        return Tool(
            "export_har",
            "Export a transaction as HAR (HTTP Archive) JSON",
            id
        ) bind { req ->
            when (val har = transactionStore.get(id(req))?.toHar()) {
                null -> Error("Transaction not found")
                else -> Json.asToolResponse(har)
            }
        }
    }
}
