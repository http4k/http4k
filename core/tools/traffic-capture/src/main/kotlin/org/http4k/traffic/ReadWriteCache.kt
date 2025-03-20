package org.http4k.traffic

import org.http4k.base64Encode
import org.http4k.connect.storage.Storage
import org.http4k.core.HttpMessage
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.parse
import java.security.MessageDigest

/**
 * Combined Read/Write storage models, optimised for retrieval.
 */
interface ReadWriteCache : Sink, Source {
    companion object {
        /**
         * Serialise and retrieve HTTP traffic to/from the FS.
         */
        fun Disk(baseDir: String = ".", shouldStore: (HttpMessage) -> Boolean = { true }): ReadWriteCache = object : ReadWriteCache,
            Source by Source.DiskTree(baseDir),
            Sink by Sink.DiskTree(baseDir, shouldStore) {}

        /**
         * Serialise and retrieve HTTP traffic to/from Memory.
         */
        fun Memory(cache: MutableMap<Request, Response> = mutableMapOf(), shouldStore: (HttpMessage) -> Boolean = { true }): ReadWriteCache = object : ReadWriteCache,
            Source by Source.MemoryMap(cache),
            Sink by Sink.MemoryMap(cache, shouldStore) {}
    }
}

/**
 * Transform a org.http4k.connect.storage.Storage object into a ReadWriteCache. Requires the http4k-connect-storage module
 */
fun Storage<String>.asCache() = asCache({ it.toString() }, { Response.parse(it) })

fun <T : Any> Storage<T>.asCache(
    marshall: (Response) -> T,
    unmarshall: (T) -> Response,
    key: (Request) -> String = { MessageDigest.getInstance("SHA256").digest(it.toString().toByteArray()).base64Encode() },
): ReadWriteCache = object : ReadWriteCache {

    override fun get(request: Request): Response? = this@asCache[key(request)]?.let(unmarshall)

    override fun set(request: Request, response: Response) {
        this@asCache[key(request)] = marshall(response)
    }
}
