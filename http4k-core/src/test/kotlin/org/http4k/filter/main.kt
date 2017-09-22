package org.http4k.filter

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Request


fun main(args: Array<String>) {
    val cacheFromDisk: Filter = TrafficFilters.SimpleCachingFrom(Traffic.Cache.Disk())
    val cacheFromMemory: Filter = TrafficFilters.SimpleCachingFrom(Traffic.Cache.Memory())

    val serveFromMemory: Filter = TrafficFilters.ServeCachedFrom(Traffic.Recall.MemoryCache(mutableMapOf()))
    val serveFromDisk: Filter = TrafficFilters.ServeCachedFrom(Traffic.Recall.DiskCache())

    val recordToMemory: Filter = TrafficFilters.RecordTo(Traffic.Storage.MemoryCache(mutableMapOf()))
    val recordToDisk: Filter = TrafficFilters.RecordTo(Traffic.Storage.DiskCache())

    val requestsFromDisk: Iterator<Request> = Replay.requestsFrom(Traffic.Replay.DiskQueue())
    val requestsFromMemory: Iterator<Request> = Replay.requestsFrom(Traffic.Replay.MemoryQueue(mutableListOf()))

    val responsesFromDisk: HttpHandler = Replay.responsesFrom(Traffic.Replay.DiskQueue())
    val responsesFromMemory: HttpHandler = Replay.responsesFrom(Traffic.Replay.MemoryQueue(mutableListOf()))

}