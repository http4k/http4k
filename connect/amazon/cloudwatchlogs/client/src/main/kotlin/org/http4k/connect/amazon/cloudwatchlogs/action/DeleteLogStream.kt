package org.http4k.connect.amazon.cloudwatchlogs.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.cloudwatchlogs.CloudWatchLogsAction
import org.http4k.connect.amazon.cloudwatchlogs.model.LogGroupName
import org.http4k.connect.amazon.cloudwatchlogs.model.LogStreamName
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
@JsonSerializable
data class DeleteLogStream(
    val logGroupName: LogGroupName,
    val logStreamName: LogStreamName
) : CloudWatchLogsAction<Unit>(Unit::class)
