package org.http4k.routing.experimental

import org.http4k.core.ContentType
import java.io.File
import java.io.InputStream
import java.time.Instant
import java.time.temporal.ChronoUnit.SECONDS

internal class FileResource(
    private val file: File,
    override val contentType: ContentType
) : Resource {

    override fun openStream(): InputStream = file.inputStream()

    override val length: Long get() = file.length()

    // We must truncate the lastModified value because the If-Last-Modified header  doesn't support fractions of seconds.
    override val lastModified: Instant get() = Instant.ofEpochMilli(file.lastModified()).truncatedTo(SECONDS)
}
