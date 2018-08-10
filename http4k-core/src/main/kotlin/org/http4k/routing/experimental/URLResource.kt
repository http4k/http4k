package org.http4k.routing.experimental

import java.io.InputStream
import java.net.URL

data class URLResource(val url: URL) : Resource {
    override fun openStream(): InputStream = url.openStream()
}