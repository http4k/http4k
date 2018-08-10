package org.http4k.routing.experimental

import java.io.InputStream
import java.net.URL
import java.time.Instant

data class URLResource(
    val url: URL,
    override val lastModified: Instant? = null
) : Resource {
    override fun openStream(): InputStream = url.openStream()
}