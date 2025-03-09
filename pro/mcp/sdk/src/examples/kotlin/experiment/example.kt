package experiment

import org.http4k.connect.model.Base64Blob
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.ContentType.Companion.APPLICATION_PDF
import org.http4k.core.Uri
import org.http4k.mcp.CompletionResponse
import org.http4k.mcp.PromptResponse
import org.http4k.mcp.ResourceRequest
import org.http4k.mcp.ResourceResponse
import org.http4k.mcp.model.Content.Text
import org.http4k.mcp.model.Message
import org.http4k.mcp.model.MimeType
import org.http4k.mcp.model.Prompt
import org.http4k.mcp.model.PromptName
import org.http4k.mcp.model.Reference
import org.http4k.mcp.model.Resource
import org.http4k.mcp.model.ResourceName
import org.http4k.mcp.model.Role.user
import org.http4k.mcp.protocol.ServerMetaData
import org.http4k.mcp.server.capability.CompletionCapability
import org.http4k.mcp.server.capability.PromptCapability
import org.http4k.routing.bind
import org.http4k.routing.mcpJsonRpc
import org.http4k.routing.mcpSse
import org.http4k.routing.mcpWebsocket
import org.http4k.server.Helidon
import org.http4k.server.asServer


fun getClaimants(): CompletionCapability = Reference.Prompt("HealthInsuranceClaim") bind { req ->
    CompletionResponse(listOf("Alice", "Bob", "Charlie", "David"))
}

fun insuranceClaim(): PromptCapability {
    val claimant = Prompt.Arg.required("claimant")
    val item = Prompt.Arg.required("item")
    return Prompt(PromptName.of("HealthInsuranceClaim"), "Raising a health insurance claim", claimant) bind { req ->
        PromptResponse(
            listOf(
                Message(
                    user, Text(
                        """To raise a claim for ${claimant(req)} expensing ${item(req)}.
                Check their purchases using the resource: purchases://${claimant(req)}.
                Raise a claim against AcmeHealthInsurance for the item and the cost.
                """.trimIndent()
                    )
                )
            )
        )
    }
}


fun getInvoiceForPurchase() = Resource.Templated(
    Uri.of("purchases://invoices/{what}"), ResourceName.of("Invoice"), "Invoice document for a purchase",
    MimeType.of(APPLICATION_PDF)
) bind { req: ResourceRequest ->
    ResourceResponse(Resource.Content.Blob(Base64Blob.encode("PDF"), req.uri, MimeType.of(APPLICATION_PDF)))
}

fun getPurchases() = Resource.Static(
    Uri.of("purchases://david"), ResourceName.of("DavidPurchases"), "List of purchases for David",
    MimeType.of(APPLICATION_JSON)
) bind { req ->
    ResourceResponse(
        listOf(
            Resource.Content.Text(
                when (req.uri.authority) {
                    "david" -> """{"purchases": ["book", "phone", "laptop"]}"""
                    else -> """{"purchases": []}"""
                },
                req.uri,
                MimeType.of(APPLICATION_JSON)
            )
        )
    )
}

val family = mcpSse(
    ServerMetaData("my family agent", "1.0.0"),
    getClaimants(),
    insuranceClaim()
).asServer(Helidon(7500)).start()

val pharmacy = mcpJsonRpc(
    ServerMetaData("CongoOnline", "1.0.0"),
    getPurchases(),
    getInvoiceForPurchase()
).asServer(Helidon(8500)).start()

val insuranceCo = mcpWebsocket(
    ServerMetaData("AcmeHealthInsurance", "1.0.0"),
    getPurchases(),
    getInvoiceForPurchase()
).asServer(Helidon(9500)).start()

fun main() {
    family
    pharmacy
    insuranceCo
}
