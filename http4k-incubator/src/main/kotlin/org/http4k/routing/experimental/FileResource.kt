package org.http4k.routing.experimental

import org.http4k.core.ContentType
import java.io.File
import java.io.InputStream
import java.time.Instant

internal class FileResource(
    private val file: File,
    override val contentType: ContentType
) : Resource {

    override fun openStream(): InputStream = file.inputStream()

    override val length: Long get() = file.length()

    override val lastModified: Instant get() = Instant.ofEpochMilli(file.lastModified())
}
