package org.http4k.mcp

import dev.forkhandles.bunting.use
import dev.forkhandles.time.executors.SimpleSchedulerService
import org.http4k.client.JavaHttpClient
import org.http4k.client.JavaSseClient
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.core.extend
import org.http4k.filter.debug
import org.http4k.mcp.internal.DebuggingReader
import org.http4k.mcp.internal.DebuggingWriter
import org.http4k.mcp.internal.pipeSseTraffic

object McpDesktop {
    @JvmStatic
    fun main(vararg args: String) = McpOptions(args.toList().toTypedArray())
        .use {
            pipeSseTraffic(
                if (debug) DebuggingReader(System.`in`.reader()) else System.`in`.reader(),
                if (debug) DebuggingWriter(System.out.writer()) else System.out.writer(),
                SimpleSchedulerService(1),
                Request(GET, Uri.of(url).extend(Uri.of("sse"))),
                if (debug) JavaHttpClient().debug(System.err) else JavaHttpClient(),
                JavaSseClient()
            )
        }
}
