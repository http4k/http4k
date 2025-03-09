package experiment

import org.http4k.connect.model.Base64Blob
import org.http4k.core.ContentType.Companion.APPLICATION_PDF
import org.http4k.core.Uri
import org.http4k.filter.debug
import org.http4k.mcp.ResourceRequest
import org.http4k.mcp.ResourceResponse
import org.http4k.mcp.ToolResponse
import org.http4k.mcp.model.Content.Text
import org.http4k.mcp.model.MimeType
import org.http4k.mcp.model.Resource
import org.http4k.mcp.model.ResourceName
import org.http4k.mcp.model.Tool
import org.http4k.mcp.protocol.ServerMetaData
import org.http4k.mcp.server.capability.ToolCapability
import org.http4k.routing.bind
import org.http4k.routing.mcpWebsocket
import org.http4k.server.Helidon
import org.http4k.server.asServer

fun raiseClaim(): ToolCapability {
    val claimant = Tool.Arg.required("claimant")
    val item = Tool.Arg.required("item")
    val invoice = Tool.Arg.required("invoice")
    return Tool("raiseClaim", "Raise a claim for items", claimant, item, invoice) bind { req ->
        ToolResponse.Ok(Text("Claim raised ${claimant(req)} for item ${item(req)} invoice ${invoice(req)}"))
    }
}

fun getInvoiceForPurchase() = Resource.Templated(
    Uri.of("purchases://invoices/{what}"), ResourceName.of("Invoice"), "Invoice document for a purchase",
    MimeType.of(APPLICATION_PDF)
) bind { req: ResourceRequest ->
    ResourceResponse(Resource.Content.Blob(Base64Blob.encode("PDF"), req.uri, MimeType.of(APPLICATION_PDF)))
}

val insuranceCo = mcpWebsocket(
    ServerMetaData("AcmeHealthInsurance", "1.0.0"),
    raiseClaim()
).debug().asServer(Helidon(9500)).start()
