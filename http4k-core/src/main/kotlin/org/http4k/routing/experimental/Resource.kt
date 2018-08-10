package org.http4k.routing.experimental

import org.http4k.core.*
import java.io.InputStream
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

interface Resource : HttpHandler {

    fun openStream(): InputStream

    val length: Long? get() = null
    val lastModified: Instant? get() = null

    fun isModifiedSince(instant: Instant): Boolean = lastModified?.isAfter(instant) ?: true

    val headers: Headers
        get() = listOf(
            "Content-Length" to length?.toString(),
            "Last-Modified" to lastModified?.formattedWith(dateTimeFormatter)
        )

    override fun invoke(request: Request): Response {
        val ifModifiedSince = request.header("If-Modified-Since").parsedWith(dateTimeFormatter)
        return if (ifModifiedSince != null && !isModifiedSince(ifModifiedSince))
            MemoryResponse(Status.NOT_MODIFIED, headers)
        else
            MemoryResponse(Status.OK, headers, Body(openStream(), length)) // Pipeline is responsible for closing stream
    }
}

private val dateTimeFormatter = DateTimeFormatter.RFC_1123_DATE_TIME.withZone(ZoneOffset.UTC)

private fun Instant.formattedWith(formatter: DateTimeFormatter) = formatter.format(this)

private fun String?.parsedWith(formatter: DateTimeFormatter): Instant? =
    if (this == null)
        null
    else try {
        Instant.from(formatter.parse(this))
    } catch (x: Exception) {
        null // "if the passed If-Modified-Since date is invalid, the response is exactly the same as for a normal GET"
    }


