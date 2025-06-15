package org.http4k.connect.amazon.cloudwatch.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.cloudwatch.CloudWatchAction
import org.http4k.connect.amazon.cloudwatch.model.AlarmName
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.core.model.Tag
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
@JsonSerializable
data class PutCompositeAlarm(
    val AlarmName: AlarmName,
    val AlarmRule: String,
    val ActionsEnabled: Boolean? = null,
    val ActionsSuppressor: String? = null,
    val ActionsSuppressorExtensionPeriod: Int? = null,
    val ActionsSuppressorWaitPeriod: Int? = null,
    val AlarmActions: List<ARN>? = null,
    val AlarmDescription: String? = null,
    val InsufficientDataActions: List<ARN>? = null,
    val OKActions: List<ARN>? = null,
    val Tags: List<Tag>? = null,
) : CloudWatchAction<Unit>(Unit::class)
