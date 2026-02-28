package org.http4k.wiretap.client

import org.http4k.core.HttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.template.DatastarElementRenderer
import org.http4k.template.TemplateRenderer
import org.http4k.wiretap.WiretapFunction
import org.http4k.wiretap.domain.Direction.Inbound
import org.http4k.wiretap.domain.TransactionStore
import java.time.Clock

fun InboundClient(
    clock: Clock,
    transactions: TransactionStore,
    proxy: HttpHandler
) = object : WiretapFunction {
    private val sendRequest = SendRequest(proxy, clock, Inbound)

    override fun http(elements: DatastarElementRenderer, html: TemplateRenderer) =
        "client" bind routes(
            sendRequest.http(elements, html),
            FormatBody(),
            HeaderRows(elements, basePath = "/__wiretap/client/"),
            Index("/", html, transactions, basePath = "/__wiretap/client/"),
        )

    override fun mcp() = sendRequest.mcp()
}
