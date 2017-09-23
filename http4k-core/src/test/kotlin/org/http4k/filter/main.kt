package org.http4k.filter

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Request


fun main(args: Array<String>) {
    val serveFromMemory: Filter = TrafficFilters.ServeCacheFrom(Traffic.Cache.Memory(cache = mutableMapOf()))
    val serveFromDisk: Filter = TrafficFilters.ServeCacheFrom(Traffic.Cache.Disk())

    val recordToMemory: Filter = TrafficFilters.RecordTo(Traffic.Cache.Memory(cache = mutableMapOf()))
    val recordToDisk: Filter = TrafficFilters.RecordTo(Traffic.Cache.Disk())

    val recordToMemoryQueue: Filter = TrafficFilters.RecordTo(Traffic.Write.MemoryStream(mutableListOf()))
    val recordToDiskQueeu: Filter = TrafficFilters.RecordTo(Traffic.Write.DiskStream())

    val requestsFromDisk: Sequence<Request> = Requester.from(Traffic.Replay.DiskStream())
    val requestsFromMemory: Sequence<Request> = Requester.from(Traffic.Replay.MemoryStream(mutableListOf()))

    val responderFromCache: HttpHandler = Responder.from(Traffic.Cache.Disk())
    val responderFromRecall: HttpHandler = Responder.from(Traffic.Cache.Disk())
}