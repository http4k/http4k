package org.http4k.filter

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.filter.TrafficFilters.RecordTo
import org.http4k.filter.TrafficFilters.ServeCachedFrom
import org.http4k.traffic.Traffic


fun main(args: Array<String>) {
    val serveFromMemory: Filter = ServeCachedFrom(Traffic.Source.MemoryMap(cache = mutableMapOf()))
    val serveFromDisk: Filter = ServeCachedFrom(Traffic.Source.DiskTree())

    val recordToMemoryMap: Filter = RecordTo(Traffic.Sink.MemoryMap(cache = mutableMapOf()))
    val recordToMemoryStream: Filter = RecordTo(Traffic.Sink.MemoryStream(stream = mutableListOf()))
    val recordToDiskStream: Filter = RecordTo(Traffic.Sink.DiskStream())
    val recordToDiskTree: Filter = RecordTo(Traffic.Sink.DiskTree())

    val responderFromDiskStream: HttpHandler = Responder.from(Traffic.ReadWriteStream.Disk())
    val responderFromMemoryStream: HttpHandler = Responder.from(Traffic.ReadWriteStream.Memory())
    val responderFromDiskCache: HttpHandler = Responder.from(Traffic.ReadWriteCache.Disk())
    val responderFromDiskMemory: HttpHandler = Responder.from(Traffic.ReadWriteCache.Memory())

    val responderFromDiskReplay: HttpHandler = Responder.from(Traffic.Replay.DiskStream())
    val responderFromMemoryReplay: HttpHandler = Responder.from(Traffic.Replay.MemoryStream(stream = mutableListOf()))
}