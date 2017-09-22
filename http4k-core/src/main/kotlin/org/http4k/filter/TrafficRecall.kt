package org.http4k.filter

import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.parse

interface TrafficRecall {
    operator fun get(request: Request): Response?

    open class DiskCache(private val baseDir: String = ".") : TrafficRecall {
        override fun get(request: Request): Response? =
            request.toFile(baseDir.toBaseFolder()).run {
                if (exists()) Response.parse(String(readBytes())) else null
            }
    }

    open class MemoryCache(private val cache: MutableMap<Request, Response>) : TrafficRecall {
        override fun get(request: Request): Response? = cache[request]
    }
}