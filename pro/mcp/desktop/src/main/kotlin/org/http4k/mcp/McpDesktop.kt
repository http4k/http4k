package org.http4k.mcp

import dev.forkhandles.bunting.use
import org.http4k.client.SseReconnectionMode.Immediate
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.mcp.TransportMode.jsonrpc
import org.http4k.mcp.TransportMode.sse
import org.http4k.mcp.internal.McpDesktopHttpClient
import org.http4k.mcp.internal.pipeJsonRpcTraffic
import org.http4k.mcp.internal.pipeSseTraffic
import org.http4k.mcp.util.DebuggingReader
import org.http4k.mcp.util.DebuggingWriter
import java.time.Clock

object McpDesktop {
    @JvmStatic
    fun main(vararg args: String) = McpOptions(args.toList().toTypedArray())
        .use {
            val clock = Clock.systemUTC()

            when (transport) {
                sse -> pipeSseTraffic(
                    if (debug) DebuggingReader(System.`in`.reader()) else System.`in`.reader(),
                    if (debug) DebuggingWriter(System.out.writer()) else System.out.writer(),
                    Request(GET, url),
                    McpDesktopHttpClient(clock, this),
                    Immediate
                )

                jsonrpc -> pipeJsonRpcTraffic(
                    if (debug) DebuggingReader(System.`in`.reader()) else System.`in`.reader(),
                    if (debug) DebuggingWriter(System.out.writer()) else System.out.writer(),
                    Request(GET, url),
                    McpDesktopHttpClient(clock, this),
                )
            }
        }
}

