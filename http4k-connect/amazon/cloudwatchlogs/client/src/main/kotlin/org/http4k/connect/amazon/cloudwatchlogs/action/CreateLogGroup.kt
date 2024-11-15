package org.http4k.connect.amazon.cloudwatchlogs.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.cloudwatchlogs.CloudWatchLogsAction
import org.http4k.connect.amazon.cloudwatchlogs.model.LogGroupName
import org.http4k.connect.amazon.core.model.KMSKeyId
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
@JsonSerializable
data class CreateLogGroup(
    val logGroupName: LogGroupName,
    val tags: Map<String, String>,
    val kmsKeyId: KMSKeyId? = null
) : CloudWatchLogsAction<Unit>(Unit::class)
