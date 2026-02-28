package org.http4k.wiretap.client

import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.lens.html
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.template.TemplateRenderer
import org.http4k.template.ViewModel
import org.http4k.wiretap.domain.TransactionStore
import org.http4k.wiretap.util.Json

data class HeaderEntry(val name: String = "", val value: String = "")

data class ClientSignals(
    val url: String = "",
    val method: String = "GET",
    val headers: Map<String, HeaderEntry> = mapOf("0" to HeaderEntry()),
    val contentType: String? = null,
    val body: String? = null
)

fun Index(
    defaultUrl: String,
    templates: TemplateRenderer,
    transactions: TransactionStore,
    basePath: String = "/__wiretap/client/",
    pageTitle: String = "Inbound Client"
): RoutingHttpHandler =
    "/" bind GET to { req ->
        val importId = req.query("import")?.toLongOrNull()
        val tx = importId?.let { transactions.get(it) }

        when (tx) {
            null -> Response(OK).html(
                templates(
                    Index(
                        ClientSignals(url = defaultUrl),
                        basePath = basePath, pageTitle = pageTitle
                    )
                )
            )

            else -> {
                val request = tx.transaction.request
                val importedHeaders = request.headers.take(10)
                val ct = request.headers
                    .firstOrNull { it.first.equals("Content-Type", ignoreCase = true) }
                    ?.second ?: ""

                val headers = when {
                    importedHeaders.isEmpty() -> mapOf("0" to HeaderEntry())
                    else -> importedHeaders.mapIndexed { index, (name, value) ->
                        index.toString() to HeaderEntry(name, value ?: "")
                    }.toMap()
                }

                val signals = ClientSignals(
                    method = request.method.name,
                    url = request.uri.toString(),
                    headers = headers,
                    contentType = ct,
                    body = request.bodyString()
                )
                Response(OK).html(
                    templates(
                        Index(
                            signals,
                            basePath,
                            pageTitle
                        )
                    )
                )
            }
        }
    }

data class Index(
    val signals: ClientSignals,
    val basePath: String = "/__wiretap/client/",
    val pageTitle: String = "Inbound Client"
) : ViewModel {
    val initialSignals: String = Json.asDatastarSignals(signals)
    val initialHeaderRows: String = renderHeaderRows(signals.headers)
}
