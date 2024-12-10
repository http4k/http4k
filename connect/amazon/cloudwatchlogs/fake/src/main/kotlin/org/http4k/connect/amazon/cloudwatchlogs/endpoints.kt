package org.http4k.connect.amazon.cloudwatchlogs

import org.http4k.connect.amazon.AwsJsonFake
import org.http4k.connect.amazon.JsonError
import org.http4k.connect.amazon.cloudwatchlogs.action.CreateLogGroup
import org.http4k.connect.amazon.cloudwatchlogs.action.CreateLogStream
import org.http4k.connect.amazon.cloudwatchlogs.action.DeleteLogGroup
import org.http4k.connect.amazon.cloudwatchlogs.action.DeleteLogStream
import org.http4k.connect.amazon.cloudwatchlogs.action.FilterLogEvents
import org.http4k.connect.amazon.cloudwatchlogs.action.FilteredLogEvent
import org.http4k.connect.amazon.cloudwatchlogs.action.FilteredLogEvents
import org.http4k.connect.amazon.cloudwatchlogs.action.PutLogEvents
import org.http4k.connect.amazon.cloudwatchlogs.action.SearchedLogStreams
import org.http4k.connect.amazon.cloudwatchlogs.model.LogGroupName
import org.http4k.connect.amazon.cloudwatchlogs.model.NextToken
import org.http4k.connect.storage.Storage
import java.lang.Integer.MAX_VALUE
import java.util.UUID


fun AwsJsonFake.createLogGroup(logGroups: Storage<LogGroup>) = route<CreateLogGroup> {
    when (logGroups[it.logGroupName.value]) {
        null -> logGroups[it.logGroupName.value] = LogGroup(mutableMapOf())

        else -> JsonError("conflict", "${it.logGroupName} already exists")
    }
}

fun AwsJsonFake.createLogStream(logGroups: Storage<LogGroup>) = route<CreateLogStream> {
    when (val existing: LogGroup? = logGroups[it.logGroupName.value]) {
        null -> JsonError("not found", "${it.logGroupName} not found")
        else -> {
            existing.streams[it.logStreamName] = mutableListOf()
        }
    }
}

fun AwsJsonFake.deletaLogStream(logGroups: Storage<LogGroup>) = route<DeleteLogStream> {
    when (val group = logGroups[it.logGroupName.value]) {
        null -> JsonError("not found", "${it.logGroupName} not found")
        else -> group.streams -= it.logStreamName
    }
}

fun AwsJsonFake.deletaLogGroup(logGroups: Storage<LogGroup>) = route<DeleteLogGroup> {
    when (logGroups[it.logGroupName.value]) {
        null -> JsonError("not found", "${it.logGroupName} not found")
        else -> logGroups -= it.logGroupName.value
    }
}

fun AwsJsonFake.putLogEvents(logGroups: Storage<LogGroup>) = route<PutLogEvents> { req ->
    val totalEventCount = logGroups.keySet().sumOf { logGroups[it]!!.streams.values.flatten().size }

    when (val group = logGroups[req.logGroupName.value]) {
        null -> JsonError("not found", "${req.logGroupName} not found")
        else -> group.streams.getOrPut(req.logStreamName) { mutableListOf() } += req.logEvents.mapIndexed { i, it ->
            FilteredLogEvent(
                UUID(req.logStreamName.hashCode().toLong(), (totalEventCount + i).toLong()).toString(),
                it.timestamp,
                req.logStreamName,
                it.message,
                it.timestamp
            )
        }
    }
}

fun AwsJsonFake.filterLogEvents(logGroups: Storage<LogGroup>) = route<FilterLogEvents> { req ->
    val group = (req.logGroupName ?: req.logGroupIdentifier?.resourceId(LogGroupName::of))
        ?.let { logGroups[it.value] }

    when (group) {
        null -> FilteredLogEvents(emptyList(), null, emptyList())
        else -> {
            val toDrop = req.nextToken?.value?.toInt() ?: 0

            val eventPredicate = when {
                req.logStreamNames != null -> {
                    { event: FilteredLogEvent -> req.logStreamNames!!.contains(event.logStreamName) }
                }

                req.logStreamNamePrefix != null -> { key: FilteredLogEvent ->
                    key.logStreamName.value.startsWith(req.logStreamNamePrefix!!)
                }

                else -> { _: FilteredLogEvent -> false }
            }

            val filteredEvents = group.streams.values.flatten().sortedBy { it.timestamp.value }
                .filter(eventPredicate)

            val results = filteredEvents.drop(toDrop).take(req.limit ?: MAX_VALUE)

            FilteredLogEvents(
                results,
                results.lastOrNull()?.eventId
                    ?.let(UUID::fromString)
                    ?.leastSignificantBits
                    ?.plus(1)
                    ?.toString()
                    ?.let(NextToken::of),
                filteredEvents.map { it.logStreamName }.toSet().map { SearchedLogStreams(it, true) }
            )
        }
    }
}
