package org.http4k.filter

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.HttpMessage
import org.http4k.core.MemoryBody
import org.http4k.core.then
import java.io.PrintStream

object DebuggingFilters {
    private const val defaultDebugStream = false

    /**
     * Print details of the request before it is sent to the next service.
     */
    object PrintRequest {
        operator fun invoke(out: PrintStream = System.out, debugStream: Boolean = defaultDebugStream): Filter = RequestFilters.Tap { req ->
            out.println(listOf("***** REQUEST: ${req.method}: ${req.uri} *****", req.printable(debugStream)).joinToString("\n"))
        }
    }

    /**
     * Print details of the response before it is returned.
     */
    object PrintResponse {
        operator fun invoke(out: PrintStream = System.out, debugStream: Boolean = defaultDebugStream): Filter = Filter { next ->
            HttpHandler {
                try {
                    next(it).let { response ->
                        out.println(listOf("***** RESPONSE ${response.status.code} to ${it.method}: ${it.uri} *****", response.printable(debugStream)).joinToString("\n"))
                        response
                    }
                } catch (e: Exception) {
                    out.println("***** RESPONSE FAILED to ${it.method}: ${it.uri} *****")
                    e.printStackTrace(out)
                    throw e
                }
            }
        }
    }

    private fun HttpMessage.printable(debugStream: Boolean) =
        if (debugStream || body is MemoryBody) this else body("<<stream>>")

    /**
     * Print details of a request and it's response.
     */
    object PrintRequestAndResponse {
        operator fun invoke(out: PrintStream = System.out, debugStream: Boolean = defaultDebugStream) = PrintRequest(out, debugStream).then(PrintResponse(out, debugStream))
    }
}
