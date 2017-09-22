package org.http4k.filter

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Request


fun main(args: Array<String>) {

    val cacheFromDisk: Filter = SimpleCaching.from(TrafficCache.Disk())
    val cacheFromMemory: Filter = SimpleCaching.from(TrafficCache.Memory())

    val serveFromMemory: Filter = ServeCachedTraffic.from(TrafficRecall.MemoryCache(mutableMapOf()))
    val serveFromDisk: Filter = ServeCachedTraffic.from(TrafficRecall.DiskCache())

    val recordToMemory: Filter = RecordTraffic.into(TrafficStorage.MemoryCache(mutableMapOf()))
    val recordToDisk: Filter = RecordTraffic.into(TrafficStorage.DiskCache())

    val requestsFromDisk: Iterator<Request> = Replay.requestsFrom(TrafficReplay.DiskQueue())
    val requestsFromMemory: Iterator<Request> = Replay.requestsFrom(TrafficReplay.MemoryQueue(mutableListOf()))

    val responsesFromDisk: HttpHandler = Replay.responsesFrom(TrafficReplay.DiskQueue())
    val responsesFromMemory: HttpHandler = Replay.responsesFrom(TrafficReplay.MemoryQueue(mutableListOf()))

}