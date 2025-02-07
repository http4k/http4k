package org.http4k.mcp

import dev.forkhandles.bunting.use
import org.http4k.client.JavaHttpClient
import org.http4k.client.SseReconnectionMode.Immediate
import org.http4k.core.BodyMode.Stream
import org.http4k.core.Filter
import org.http4k.core.Method.GET
import org.http4k.core.NoOp
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.core.extend
import org.http4k.core.then
import org.http4k.filter.DebuggingFilters
import org.http4k.mcp.internal.pipeSseTraffic
import org.http4k.mcp.util.DebuggingReader
import org.http4k.mcp.util.DebuggingWriter

object McpDesktop {
    @JvmStatic
    fun main(vararg args: String) = McpOptions(args.toList().toTypedArray())
        .use {
            pipeSseTraffic(
                if (debug) DebuggingReader(System.`in`.reader()) else System.`in`.reader(),
                if (debug) DebuggingWriter(System.out.writer()) else System.out.writer(),
                Request(GET, Uri.of(url).extend(Uri.of("sse"))),
                (if (debug) DebuggingFilters.PrintRequestAndResponse(System.err, true) else Filter.NoOp)
                    .then(JavaHttpClient(responseBodyMode = Stream)),
                Immediate
            )
        }
}
