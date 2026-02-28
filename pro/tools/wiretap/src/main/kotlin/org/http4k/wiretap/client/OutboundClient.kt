package org.http4k.wiretap.client

import org.http4k.core.HttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.template.DatastarElementRenderer
import org.http4k.template.TemplateRenderer
import org.http4k.wiretap.WiretapFunction
import org.http4k.wiretap.domain.Direction.Outbound
import org.http4k.wiretap.domain.TransactionStore
import java.time.Clock

fun OutboundClient(
    httpClient: HttpHandler,
    clock: Clock,
    transactions: TransactionStore,
) = object : WiretapFunction {
    private val sendRequest = SendRequest(httpClient, clock, Outbound)

    override fun http(elements: DatastarElementRenderer, html: TemplateRenderer) =
        "outbound" bind routes(
            sendRequest.http(elements, html),
            FormatBody(),
            HeaderRows(elements, "/_wiretap/outbound/"),
            Index("", html, transactions, "/_wiretap/outbound/", pageTitle = "Outbound Client"),
        )

    override fun mcp() = sendRequest.mcp()
}
