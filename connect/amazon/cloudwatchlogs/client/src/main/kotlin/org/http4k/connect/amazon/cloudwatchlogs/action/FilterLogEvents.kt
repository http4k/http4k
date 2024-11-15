package org.http4k.connect.amazon.cloudwatchlogs.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.Paged
import org.http4k.connect.PagedAction
import org.http4k.connect.amazon.cloudwatchlogs.CloudWatchLogsAction
import org.http4k.connect.amazon.cloudwatchlogs.model.LogGroupName
import org.http4k.connect.amazon.cloudwatchlogs.model.LogStreamName
import org.http4k.connect.amazon.cloudwatchlogs.model.NextToken
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.model.TimestampMillis
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
@JsonSerializable
@ExposedCopyVisibility
data class FilterLogEvents internal constructor(
    val logGroupName: LogGroupName? = null,
    val logGroupIdentifier: ARN? = null,
    val logStreamNames: List<LogStreamName>? = null,
    val logStreamNamePrefix: String? = null,
    val nextToken: NextToken? = null,
    val startTime: TimestampMillis? = null,
    val endTime: TimestampMillis? = null,
    val unmask: Boolean = false,
    val filterPattern: String? = null,
    val limit: Int? = null,
) : CloudWatchLogsAction<FilteredLogEvents>(FilteredLogEvents::class),
    PagedAction<NextToken, FilteredLogEvent, FilteredLogEvents, FilterLogEvents> {
    constructor(
        logGroupName: LogGroupName,
        unmask: Boolean = false,
        logStreamNames: List<LogStreamName>? = null,
        logStreamNamePrefix: String? = null,
        nextToken: NextToken? = null,
        startTime: TimestampMillis? = null,
        endTime: TimestampMillis? = null,
        filterPattern: String? = null,
        limit: Int? = null
    ) : this(
        logGroupName,
        null,
        logStreamNames,
        logStreamNamePrefix,
        nextToken,
        startTime,
        endTime,
        unmask,
        filterPattern,
        limit
    )

    constructor(
        logGroupIdentifier: ARN,
        unmask: Boolean = false,
        logStreamNames: List<LogStreamName>? = null,
        logStreamNamePrefix: String? = null,
        nextToken: NextToken? = null,
        startTime: TimestampMillis? = null,
        endTime: TimestampMillis? = null,
        filterPattern: String? = null,
        limit: Int? = null
    ) : this(
        null,
        logGroupIdentifier,
        logStreamNames,
        logStreamNamePrefix,
        nextToken,
        startTime,
        endTime,
        unmask,
        filterPattern,
        limit
    )

    override fun next(token: NextToken): FilterLogEvents = copy(nextToken = token)
}

@JsonSerializable
data class FilteredLogEvent(
    val eventId: String?,
    val ingestionTime: TimestampMillis,
    val logStreamName: LogStreamName,
    val message: String,
    val timestamp: TimestampMillis
)

@JsonSerializable
data class SearchedLogStreams(
    val logStreamName: LogStreamName,
    val searchedCompletely: Boolean
)

@JsonSerializable
data class FilteredLogEvents(
    internal val events: List<FilteredLogEvent>,
    val nextToken: NextToken?,
    val searchedLogStreams: List<SearchedLogStreams>
) : Paged<NextToken, FilteredLogEvent> {
    override fun token() = nextToken

    override val items = events
}
