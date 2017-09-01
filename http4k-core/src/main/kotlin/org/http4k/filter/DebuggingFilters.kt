package org.http4k.filter

import org.http4k.core.Filter
import org.http4k.core.HttpMessage
import org.http4k.core.StreamBody
import org.http4k.core.then
import java.io.PrintStream

object DebuggingFilters {

    /**
     * Print details of the request before it is sent to the next service.
     */
    fun PrintRequest(out: PrintStream = System.out): Filter = RequestFilters.Tap { req ->
        out.println(listOf("***** REQUEST: ${req.method}: ${req.uri} *****", req.printable()).joinToString("\n"))
    }

    /**
     * Print details of the response before it is returned.
     */
    fun PrintResponse(out: PrintStream = System.out): Filter = Filter { next ->
        {
            try {
                next(it).let { response ->
                    out.println(listOf("***** RESPONSE ${response.status.code} to ${it.method}: ${it.uri} *****", response.printable()).joinToString("\n"))
                    response
                }
            } catch (e: Exception) {
                out.println("***** RESPONSE FAILED to ${it.method}: ${it.uri}  *****")
                e.printStackTrace(out)
                throw e
            }
        }
    }

    private fun HttpMessage.printable(): HttpMessage = if (body is StreamBody) body("<<stream>>") else this

    /**
     * Print details of a request and it's response.
     */
    fun PrintRequestAndResponse(out: PrintStream = System.out) = PrintRequest(out).then(PrintResponse(out))
}