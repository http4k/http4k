package org.http4k.filter

import org.http4k.core.HttpMessage
import org.http4k.core.Request
import org.http4k.core.Response

interface TrafficCache : TrafficStorage, TrafficRecall {
    companion object {
        fun Disk(baseDir: String = ".",
                 shouldStore: (HttpMessage) -> Boolean = { true }): TrafficCache {
            val recall = TrafficRecall.DiskCache(baseDir)
            val storage = TrafficStorage.DiskCache(baseDir, shouldStore)
            return object : TrafficCache, TrafficStorage by storage, TrafficRecall by recall {}
        }

        fun Memory(cache: MutableMap<Request, Response> = mutableMapOf(),
                   shouldStore: (HttpMessage) -> Boolean = { true }): TrafficCache {
            val recall = TrafficRecall.MemoryCache(cache)
            val storage = TrafficStorage.MemoryCache(cache, shouldStore)
            return object : TrafficCache, TrafficStorage by storage, TrafficRecall by recall {}
        }
    }
}