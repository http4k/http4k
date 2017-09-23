package org.http4k.filter

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Request


fun main(args: Array<String>) {
    val serveFromMemory: Filter = TrafficFilters.ServeCacheFrom(Traffic.Storage.MemoryCache(mutableMapOf()))
    val serveFromDisk: Filter = TrafficFilters.ServeCacheFrom(Traffic.Storage.DiskCache())

    val recordToMemory: Filter = TrafficFilters.RecordTo(Traffic.Storage.MemoryCache(mutableMapOf()))
    val recordToDisk: Filter = TrafficFilters.RecordTo(Traffic.Storage.DiskCache())

    val recordToMemoryQueue: Filter = TrafficFilters.RecordTo(Traffic.Storage.MemoryQueue(mutableListOf()))
    val recordToDiskQueeu: Filter = TrafficFilters.RecordTo(Traffic.Storage.DiskQueue())

    val requestsFromDisk: Iterator<Request> = Requester.from(Traffic.TrafficStream.DiskQueue())
    val requestsFromMemory: Iterator<Request> = Requester.from(Traffic.TrafficStream.MemoryQueue(mutableListOf()))

    val responderFromCache: HttpHandler = Responder.from(Traffic.Storage.DiskCache())
    val responderFromRecall: HttpHandler = Responder.from(Traffic.Storage.DiskCache())
}