package org.http4k.routing.experimental

import org.http4k.core.ContentType
import java.io.InputStream
import java.net.URL
import java.time.Instant

data class URLResource(
    val url: URL,
    override val contentType: ContentType,
    override val lastModified: Instant? = null
) : Resource {
    override fun openStream(): InputStream = url.openStream()
}
