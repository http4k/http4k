package org.http4k.routing

import java.io.InputStream
import java.net.URL
import java.time.Instant

interface Resource {
    fun toStream(): InputStream
    val length: Long? get() = null
    val lastModified: Instant? get() = null
    val expires: Instant? get() = null
    val etag: String? get() = null
}

data class URLResource(val url: URL) : Resource {
    override fun toStream(): InputStream = url.openStream()
}

fun URL.toResource(): Resource  = URLResource(this)