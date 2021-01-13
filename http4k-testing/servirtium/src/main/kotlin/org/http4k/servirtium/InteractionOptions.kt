package org.http4k.servirtium

import org.http4k.core.ContentType
import org.http4k.core.Filter
import org.http4k.core.NoOp
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.filter.DebuggingFilters

/**
 * General controls for the Servirtium interactions and how they are recorded to the storage format. The
 * manipulations are used to replace/remove any dynamic parts of the request (eg. "Date" header) so that the
 * traffic can be correctly matched during the replay process.
 */
interface InteractionOptions {

    /**
     * Modify received requests before they are stored. Use this to replace/remove dynamic parts of the message
     * before serialisation.
     */
    fun modify(request: Request): Request = request

    /**
     * Modify received responses before they are stored. Use this to replace/remove dynamic parts of the message
     * before serialisation.
     */
    fun modify(response: Response): Response = response

    /**
     * Determine if the content type from a message should be treated as binary.
     */
    fun isBinary(contentType: ContentType?): Boolean = false

    /**
     * Turn on/off the printing of raw HTTP traffic to the console.
     */
    fun debugTraffic() = false

    companion object {
        /**
         * By default, no modifications are made to the raw traffic before it gets output to disk. This will
         * not be used very often as dynamic headers such as "Date" and "User-Agent" will almost always be present and
         * need to be stripped out.
         */
        @JvmField
        val Defaults = object : InteractionOptions {}
    }
}

fun InteractionOptions.trafficPrinter() = when {
    debugTraffic() -> DebuggingFilters.PrintRequestAndResponse()
    else -> Filter.NoOp
}
