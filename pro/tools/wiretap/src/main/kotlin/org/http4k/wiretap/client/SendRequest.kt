package org.http4k.wiretap.client

import org.http4k.ai.mcp.ToolResponse.Ok
import org.http4k.ai.mcp.model.Content.Text
import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.model.enum
import org.http4k.ai.mcp.model.string
import org.http4k.ai.mcp.model.uri
import org.http4k.ai.mcp.server.capability.ToolCapability
import org.http4k.core.HttpHandler
import org.http4k.core.HttpTransaction
import org.http4k.core.Method
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.datastar.Selector
import org.http4k.lens.datastarElements
import org.http4k.routing.bind
import org.http4k.template.DatastarElementRenderer
import org.http4k.wiretap.WiretapFunction
import org.http4k.wiretap.domain.Direction
import org.http4k.wiretap.domain.TransactionDetail
import org.http4k.wiretap.domain.WiretapTransaction
import org.http4k.wiretap.domain.toDetail
import org.http4k.wiretap.traffic.TransactionDetailView
import org.http4k.wiretap.util.Json
import org.http4k.wiretap.util.Json.datastarModel
import java.time.Clock
import java.time.Duration

data class ClientRequest(
    val method: Method = GET,
    val url: Uri = Uri.of("/"),
    val headers: Map<String, HeaderEntry> = mapOf("0" to HeaderEntry()),
    val contentType: String = "",
    val body: String = ""
)

fun SendRequest(proxy: HttpHandler, clock: Clock, direction: Direction) =
    object : WiretapFunction {
        private fun sendRequest(
            method: Method, url: Uri, headers: List<Pair<String, String>>, body: String?
        ): TransactionDetail {
            val request = headers.fold(
                Request(method, url)
                    .header("x-http4k-wiretap", "replay")
                    .body(body ?: "")
            ) { acc, (k, v) -> acc.header(k, v) }

            val start = clock.instant()
            val response = proxy(request)
            val duration = Duration.between(start, clock.instant())

            return WiretapTransaction(
                id = 0,
                transaction = HttpTransaction(
                    request = request,
                    response = response,
                    duration = duration,
                    start = start
                ),
                direction = direction
            ).toDetail()
        }

        override fun http(renderer: DatastarElementRenderer) =
            "/send" bind POST to { req ->
                val model = req.datastarModel<ClientRequest>()

                val headers = model.headers.values
                    .filter { it.name.isNotBlank() }
                    .map { it.name to it.value }
                    .let { list ->
                        if (model.contentType.isNotBlank() && list.none {
                                it.first.equals(
                                    "Content-Type",
                                    ignoreCase = true
                                )
                            })
                            list + ("Content-Type" to model.contentType)
                        else list
                    }

                val body = model.body.ifBlank { null }
                val detail = sendRequest(model.method, model.url, headers, body)

                val view = TransactionDetailView(detail, showImport = false)
                Response(OK).datastarElements(
                    renderer(view),
                    selector = Selector.of("#detail-panel")
                )
            }

        override fun mcp(): ToolCapability {
            val method = Tool.Arg.enum<Method>().required("method", "HTTP method (GET, POST, PUT, DELETE, etc)")
            val url = Tool.Arg.uri().required("url", "URL to send the request to")
            val headers = Tool.Arg.string().multi.optional("headers", "Headers as key=value pairs")
            val body = Tool.Arg.string().optional("body", "Request body")

            return Tool(
                "send_request",
                "Send an HTTP request through the Wiretap proxy chain so it gets recorded",
                method, url, headers, body
            ) bind { req ->
                val headerList = headers(req)?.map { h ->
                    val (k, v) = h.split("=", limit = 2)
                    k to (v.ifEmpty { "" })
                } ?: emptyList()

                Ok(
                    listOf(
                        Text(
                            Json.asFormatString(
                                sendRequest(
                                    method(req),
                                    url(req),
                                    headerList,
                                    body(req)
                                )
                            )
                        )
                    )
                )
            }
        }
    }
