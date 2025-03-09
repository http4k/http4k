package experiment

import org.http4k.connect.model.Base64Blob
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.ContentType.Companion.APPLICATION_PDF
import org.http4k.core.Uri
import org.http4k.filter.debug
import org.http4k.mcp.ResourceRequest
import org.http4k.mcp.ResourceResponse
import org.http4k.mcp.model.MimeType
import org.http4k.mcp.model.Resource
import org.http4k.mcp.model.ResourceName
import org.http4k.mcp.protocol.ServerMetaData
import org.http4k.routing.bind
import org.http4k.routing.mcpJsonRpc
import org.http4k.server.Helidon
import org.http4k.server.asServer


fun getPurchases() = Resource.Static(
    Uri.of("purchases://david"), ResourceName.of("DavidPurchases"), "List of purchases for David",
    MimeType.of(APPLICATION_JSON)
) bind { req ->
    ResourceResponse(
        listOf(
            Resource.Content.Text(
                when (req.uri.authority) {
                    "david" -> """[{"name": "contact lenses", "cost": "£50"}]"""
                    else -> """[]]"""
                },
                req.uri,
                MimeType.of(APPLICATION_JSON)
            )
        )
    )
}

fun getInvoiceForPurchase() = Resource.Templated(
    Uri.of("purchases://invoices/{what}"), ResourceName.of("Invoice"), "Invoice document for a purchase",
    MimeType.of(APPLICATION_PDF)
) bind { req: ResourceRequest ->
    ResourceResponse(Resource.Content.Blob(Base64Blob.encode("PDF"), req.uri, MimeType.of(APPLICATION_PDF)))
}

val ecommerce = mcpJsonRpc(
    ServerMetaData("CongoDotCom", "1.0.0"),
    getPurchases(),
    getInvoiceForPurchase()
).asServer(Helidon(8500)).start()
