package org.http4k.filter

import org.http4k.core.HttpMessage
import org.http4k.core.MemoryBody
import org.http4k.core.RequestContext
import org.http4k.core.Store
import org.http4k.routing.ws.RoutingWsHandler
import org.http4k.websocket.Websocket
import org.http4k.websocket.WsFilter
import org.http4k.websocket.WsHandler
import org.http4k.websocket.WsMessage
import org.http4k.websocket.WsMessage.Mode.Binary
import org.http4k.websocket.WsMessage.Mode.Text
import org.http4k.websocket.WsResponse
import org.http4k.websocket.WsStatus
import org.http4k.websocket.then
import java.io.PrintStream

fun ServerFilters.CatchAllWs(
    onError: (Throwable) -> WsResponse = ::originalWsBehaviour,
) = WsFilter { next ->
    {
        try {
            next(it)
        } catch (e: Throwable) {
            onError(e)
        }
    }
}

private fun originalWsBehaviour(e: Throwable): WsResponse {
    if (e !is Exception) throw e
    e.printStackTrace()
    return WsResponse { it.close(WsStatus.ABNORMAL_CLOSE) }
}

fun ServerFilters.InitialiseWsRequestContext(contexts: Store<RequestContext>) = WsFilter { next ->
    {
        val context = RequestContext()
        try {
            next(contexts(context, it))
        } finally {
            contexts.remove(context)
        }
    }
}

fun ServerFilters.SetWsSubProtocol(subprotocol: String) = WsFilter { next ->
    {
        next(it).copy(subprotocol = subprotocol)
    }
}

fun DebuggingFilters.PrintWsRequest(out: PrintStream = System.out, debugStream: Boolean = false) =
    WsFilter { next ->
        { req ->
            out.println(
                listOf(
                    "***** WS REQUEST: ${req.method}: ${req.uri} *****",
                    req.printable(debugStream)
                ).joinToString("\n")
            )
            next(req)
        }
    }

fun DebuggingFilters.PrintWsRequestAndResponse(out: PrintStream = System.out, debugStream: Boolean = false) =
    PrintWsRequest(out, debugStream).then(PrintWsResponse(out))

fun WsHandler.debug(out: PrintStream = System.out, debugStream: Boolean = false) =
    DebuggingFilters.PrintWsRequestAndResponse(out, debugStream).then(this)

fun RoutingWsHandler.debug(out: PrintStream = System.out, debugStream: Boolean = false) =
    DebuggingFilters.PrintWsRequestAndResponse(out, debugStream).then(this)

fun DebuggingFilters.PrintWsResponse(out: PrintStream = System.out, debugStream: Boolean = false) =
    WsFilter { next ->
        { req ->
            try {
                next(req).let { response ->
                    out.println("***** WS RESPONSE ${response.subprotocol} to ${req.method}: ${req.uri} *****")

                    response.copy(consumer = { ws ->
                        response.consumer(object : Websocket by ws {
                            override fun send(message: WsMessage) {
                                ws.send(message)
                                out.println(
                                    "***** WS SEND ${req.method}: ${req.uri} -> " + when (message.mode) {
                                        Text -> "Text: ${message.bodyString()}"
                                        Binary -> "Binary: ${
                                            if (debugStream) message.body.payload.array().contentToString()
                                            else "<<stream>>"
                                        }"
                                    }
                                )
                            }

                            override fun close(status: WsStatus) {
                                ws.close()
                                out.println("***** WS CLOSED with ${status.code} on ${req.method}: ${req.uri} *****")
                            }
                        })
                    })
                }
            } catch (e: Exception) {
                out.println("***** WS RESPONSE FAILED to ${req.method}: ${req.uri} *****")
                e.printStackTrace(out)
                throw e
            }
        }
    }

private fun HttpMessage.printable(debugStream: Boolean) =
    if (debugStream || body is MemoryBody) this else body("<<stream>>")
