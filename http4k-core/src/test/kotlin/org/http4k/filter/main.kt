package org.http4k.filter

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Request


fun main(args: Array<String>) {
    val serveFromMemory: Filter = TrafficFilters.ServeCachedFrom(Traffic.Source.MemoryMap(cache = mutableMapOf()))
    val serveFromDisk: Filter = TrafficFilters.ServeCachedFrom(Traffic.Source.DiskTree())

    val recordToMemoryMap: Filter = TrafficFilters.RecordTo(Traffic.Sink.MemoryMap(cache = mutableMapOf()))
    val recordToMemoryStream: Filter = TrafficFilters.RecordTo(Traffic.Sink.MemoryStream(stream = mutableListOf()))
    val recordToDiskStream: Filter = TrafficFilters.RecordTo(Traffic.Sink.DiskStream())
    val recordToDiskTree: Filter = TrafficFilters.RecordTo(Traffic.Sink.DiskTree())

    val responderFromDiskStream: HttpHandler = Responder.from(Traffic.ReadWriteStream.Disk())
    val responderFromMemoryStream: HttpHandler = Responder.from(Traffic.ReadWriteStream.Memory())
    val responderFromDiskCache: HttpHandler = Responder.from(Traffic.ReadWriteCache.Disk())
    val responderFromDiskMemory: HttpHandler = Responder.from(Traffic.ReadWriteCache.Memory())

    val responderFromDiskReplay: HttpHandler = Responder.from(Traffic.Replay.DiskStream())
    val responderFromMemoryReplay: HttpHandler = Responder.from(Traffic.Replay.MemoryStream(stream = mutableListOf()))
}