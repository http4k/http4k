package org.http4k.routing.experimental

import java.io.ByteArrayInputStream
import java.time.Instant

class InMemoryResource(
    private val content: ByteArray,
    override val lastModified: Instant? = null
) : Resource {

    constructor(
        content: String,
        lastModified: Instant? = null
    ): this(content.toByteArray(Charsets.UTF_8), lastModified)

    override fun toStream() = ByteArrayInputStream(content)

    override val length: Long? = content.size.toLong()
}