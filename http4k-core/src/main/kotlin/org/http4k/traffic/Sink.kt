package org.http4k.traffic

import org.http4k.core.HttpMessage
import org.http4k.core.Request
import org.http4k.core.Response
import java.io.File
import java.util.UUID


/**
 * Consumes HTTP traffic for storage.
 */
fun interface Sink {
    operator fun set(request: Request, response: Response)

    companion object {
        /**
         * Serialises HTTP traffic to the FS, optimised for retrieval.
         */
        fun DiskTree(baseDir: String = ".", shouldStore: (HttpMessage) -> Boolean = { true }) = Sink { request, response ->
            val requestFolder = request.toFolder(baseDir.toBaseFolder())
            if (shouldStore(request)) request.writeTo(requestFolder)
            if (shouldStore(response)) response.writeTo(requestFolder)
        }

        /**
         * Serialises HTTP traffic in Memory, optimised for retrieval.
         */
        fun MemoryMap(cache: MutableMap<Request, Response>,
                      shouldStore: (HttpMessage) -> Boolean = { true }) = Sink { request, response -> if (shouldStore(request) || shouldStore(response)) cache += request to response }

        /**
         * Serialises HTTP traffic to the FS in order.
         */
        fun DiskStream(baseDir: String = ".",
                       shouldStore: (HttpMessage) -> Boolean = { true },
                       id: () -> String = { System.nanoTime().toString() + UUID.randomUUID().toString() }) = Sink { request, response ->
            val folder = File(baseDir, id())
            if (shouldStore(request)) request.writeTo(folder)
            if (shouldStore(response)) response.writeTo(folder)
        }

        /**
         * Serialises HTTP traffic to Memory in order.
         */
        fun MemoryStream(stream: MutableList<Pair<Request, Response>>,
                         shouldStore: (HttpMessage) -> Boolean = { true }) = Sink { request, response -> if (shouldStore(request) || shouldStore(response)) stream += request to response }
    }
}
