package org.http4k.mcp

import dev.forkhandles.bunting.use
import org.http4k.client.JavaHttpClient
import org.http4k.client.SseReconnectionMode.Delayed
import org.http4k.client.SseReconnectionMode.Immediate
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.mcp.TransportMode.jsonrpc
import org.http4k.mcp.TransportMode.sse
import org.http4k.mcp.TransportMode.websocket
import org.http4k.mcp.internal.McpClientSecurity
import org.http4k.mcp.internal.McpDesktopHttpClient
import org.http4k.mcp.internal.pipeJsonRpcTraffic
import org.http4k.mcp.internal.pipeSseTraffic
import org.http4k.mcp.internal.pipeWebsocketTraffic
import java.time.Clock

object McpDesktop {
    @JvmStatic
    fun main(vararg args: String) = McpOptions(args.toList().toTypedArray())
        .use {
            val clock = Clock.systemUTC()

            val security = McpClientSecurity.from(this, clock, JavaHttpClient())
            when (transport) {
                sse -> pipeSseTraffic(
                    System.`in`.reader(),
                    System.out.writer(),
                    Request(GET, url),
                    McpDesktopHttpClient(clock, security),
                    if (reconnectDelay.isZero) Immediate else Delayed(reconnectDelay),
                )

                jsonrpc -> pipeJsonRpcTraffic(
                    System.`in`.reader(),
                    System.out.writer(),
                    Request(GET, url),
                    McpDesktopHttpClient(clock, security),
                )

                websocket -> pipeWebsocketTraffic(
                    System.`in`.reader(),
                    System.out.writer(),
                    url,
                    security
                )
            }
        }
}

