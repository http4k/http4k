package org.http4k.connect.amazon.cloudwatchlogs.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.cloudwatchlogs.CloudWatchLogsAction
import org.http4k.connect.amazon.cloudwatchlogs.model.LogGroupName
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
@JsonSerializable
data class DeleteLogGroup(
    val logGroupName: LogGroupName
) : CloudWatchLogsAction<Unit>(Unit::class)
