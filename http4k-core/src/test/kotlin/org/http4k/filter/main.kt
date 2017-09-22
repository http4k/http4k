package org.http4k.filter

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Request


fun main(args: Array<String>) {
    val cacheFromDisk: Filter = SimpleCaching.from(Traffic.Cache.Disk())
    val cacheFromMemory: Filter = SimpleCaching.from(Traffic.Cache.Memory())

    val serveFromMemory: Filter = ServeCachedTraffic.from(Traffic.Recall.MemoryCache(mutableMapOf()))
    val serveFromDisk: Filter = ServeCachedTraffic.from(Traffic.Recall.DiskCache())

    val recordToMemory: Filter = RecordTraffic.into(Traffic.Storage.MemoryCache(mutableMapOf()))
    val recordToDisk: Filter = RecordTraffic.into(Traffic.Storage.DiskCache())

    val requestsFromDisk: Iterator<Request> = Replay.requestsFrom(Traffic.Replay.DiskQueue())
    val requestsFromMemory: Iterator<Request> = Replay.requestsFrom(Traffic.Replay.MemoryQueue(mutableListOf()))

    val responsesFromDisk: HttpHandler = Replay.responsesFrom(Traffic.Replay.DiskQueue())
    val responsesFromMemory: HttpHandler = Replay.responsesFrom(Traffic.Replay.MemoryQueue(mutableListOf()))

}