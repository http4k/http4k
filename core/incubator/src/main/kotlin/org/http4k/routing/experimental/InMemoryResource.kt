package org.http4k.routing.experimental

import org.http4k.core.ContentType
import org.http4k.core.etag.ETag
import java.io.ByteArrayInputStream
import java.time.Instant

class InMemoryResource(
    private val content: ByteArray,
    override val contentType: ContentType,
    override val lastModified: Instant? = null,
    override val etag: ETag? = null
) : Resource {

    constructor(
        content: String,
        contentType: ContentType,
        lastModified: Instant? = null,
        etag: ETag? = null
    ) : this(content.toByteArray(Charsets.UTF_8), contentType, lastModified, etag)

    override fun openStream() = ByteArrayInputStream(content)

    override val length: Long? = content.size.toLong()
}
