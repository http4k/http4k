package org.http4k.connect.amazon.cloudwatch.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.cloudwatch.CloudWatchAction
import org.http4k.connect.amazon.cloudwatch.model.AlarmName
import org.http4k.connect.amazon.cloudwatch.model.AlarmState
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
@JsonSerializable
data class SetAlarmState(
    val AlarmName: AlarmName,
    val StateValue: AlarmState,
    val StateReason: String,
    val StateReasonData: String? = null,
): CloudWatchAction<Unit>(Unit::class)
