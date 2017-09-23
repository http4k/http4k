package org.http4k.filter

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Request


fun main(args: Array<String>) {
    val serveFromMemory: Filter = TrafficFilters.ServeCachedFrom(Traffic.Storage.MemoryCache(mutableMapOf()))
    val serveFromDisk: Filter = TrafficFilters.ServeCachedFrom(Traffic.Storage.DiskCache())

    val recordToMemory: Filter = TrafficFilters.RecordTo(Traffic.Storage.MemoryCache(mutableMapOf()))
    val recordToDisk: Filter = TrafficFilters.RecordTo(Traffic.Storage.DiskCache())

    val recordToMemoryQueue: Filter = TrafficFilters.RecordTo(Traffic.Storage.MemoryQueue(mutableListOf()))
    val recordToDiskQueeu: Filter = TrafficFilters.RecordTo(Traffic.Storage.DiskQueue())

    val requestsFromDisk: Iterator<Request> = Requester.from(Traffic.Replay.DiskQueue())
    val requestsFromMemory: Iterator<Request> = Requester.from(Traffic.Replay.MemoryQueue(mutableListOf()))

    val responderFromCache: HttpHandler = Responder.from(Traffic.Storage.DiskCache())
    val responderFromRecall: HttpHandler = Responder.from(Traffic.Storage.DiskCache())
}