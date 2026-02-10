package experiment

import org.http4k.connect.model.MimeType
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.Uri
import org.http4k.jsonrpc.ErrorMessage.Companion.InvalidRequest
import org.http4k.ai.mcp.ResourceResponse
import org.http4k.ai.mcp.ToolRequest
import org.http4k.ai.mcp.ToolResponse
import org.http4k.ai.mcp.model.Content
import org.http4k.ai.mcp.model.Resource
import org.http4k.ai.mcp.model.ResourceName
import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.model.int
import org.http4k.ai.mcp.protocol.McpException
import org.http4k.ai.mcp.protocol.ServerMetaData
import org.http4k.ai.mcp.server.capability.ToolCapability
import org.http4k.ai.mcp.server.security.NoMcpSecurity
import org.http4k.routing.bind
import org.http4k.routing.mcpHttpStreaming
import org.http4k.server.JettyLoom
import org.http4k.server.asServer


fun getPurchases() = Resource.Static(
    Uri.of("purchases://david"), ResourceName.of("DavidPurchases"), "List of purchases for David",
    MimeType.of(APPLICATION_JSON)
) bind { req ->
    ResourceResponse(
        listOf(
            Resource.Content.Text(
                when (req.uri.authority) {
                    "david" -> """[{"name": "contact lenses","invoice-id":1, "cost": "£50"}]"""
                    else -> """[]]"""
                },
                Uri.of("purchases://invoices/contacts"),
                MimeType.of(APPLICATION_JSON)
            )
        )
    )
}

fun getInvoiceForPurchase(): ToolCapability {
    val invoiceId = Tool.Arg.int().required("invoice-id", "invoice id")
    return Tool(
        "getInvoiceForPurchase",
        "Get invoice for a purchase",
        invoiceId
    ) bind { req: ToolRequest ->
        when (invoiceId(req)) {
            1 -> ToolResponse.Ok(Content.Text("A receipt for some contact lenses"))
            else -> throw McpException(InvalidRequest)
        }
    }
}

val congoDotCom = mcpHttpStreaming(
    ServerMetaData("CongoDotCom", "1.0.0"),
    NoMcpSecurity,
    getPurchases(),
    getInvoiceForPurchase()
).asServer(JettyLoom(8500)).start()
