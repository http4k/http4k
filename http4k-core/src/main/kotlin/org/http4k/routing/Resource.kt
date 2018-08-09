package org.http4k.routing

import org.http4k.core.*
import java.io.InputStream
import java.net.URL
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

interface Resource {
    fun toStream(): InputStream

    val length: Long? get() = null
    val lastModified: Instant? get() = null

    val headers: Headers
        get() = listOf(
            "Content-Length" to length?.toString(),
            "Last-Modified" to lastModified?.formattedWith(dateTimeFormatter)
        )

    fun response(): Response = toStream().use { inputStream ->
        MemoryResponse(Status.OK, headers, Body(inputStream, length))
    }
}

private val dateTimeFormatter = DateTimeFormatter.RFC_1123_DATE_TIME.withZone(ZoneOffset.UTC)

private fun Instant.formattedWith(formatter: DateTimeFormatter) = formatter.format(this)


data class URLResource(val url: URL) : Resource {
    override fun toStream(): InputStream = url.openStream()
}

fun URL.toResource(): Resource = URLResource(this)