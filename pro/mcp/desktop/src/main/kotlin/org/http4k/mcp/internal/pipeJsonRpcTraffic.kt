package org.http4k.mcp.internal

import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.HttpHandler
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.lens.contentType
import java.io.Reader
import java.io.Writer
import kotlin.concurrent.thread

/**
 * Connect to the HTTP endpoint, constructing the request using the passed function
 */
fun pipeJsonRpcTraffic(input: Reader, output: Writer, sseRequest: Request, http: HttpHandler) {
    thread {
        input.buffered().lineSequence().forEach { next ->
            runCatching {
                val response = http(
                    Request(POST, sseRequest.uri)
                        .contentType(APPLICATION_JSON)
                        .body(next)
                )
                output.apply { write("${response.bodyString()}\n") }.flush()
            }
                .onFailure { it.printStackTrace(System.err) }
        }
    }
}
