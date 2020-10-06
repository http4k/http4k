package org.http4k.traffic

import org.http4k.core.HttpMessage
import org.http4k.core.Request
import org.http4k.core.Response

/**
 * Combined Read/Write storage models, optimised for retrieval.
 */
interface ReadWriteCache : Sink, Source {
    companion object {
        /**
         * Serialise and retrieve HTTP traffic to/from the FS.
         */
        fun Disk(baseDir: String = ".", shouldStore: (HttpMessage) -> Boolean = { true }): ReadWriteCache = object :
            ReadWriteCache,
            Source by Source.DiskTree(baseDir),
            Sink by Sink.DiskTree(baseDir, shouldStore) {}

        /**
         * Serialise and retrieve HTTP traffic to/from Memory.
         */
        fun Memory(cache: MutableMap<Request, Response> = mutableMapOf(), shouldStore: (HttpMessage) -> Boolean = { true }): ReadWriteCache = object :
            ReadWriteCache,
            Source by Source.MemoryMap(cache),
            Sink by Sink.MemoryMap(cache, shouldStore) {}
    }
}
