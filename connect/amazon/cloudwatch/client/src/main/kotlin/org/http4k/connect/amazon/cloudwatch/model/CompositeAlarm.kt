package org.http4k.connect.amazon.cloudwatch.model

import org.http4k.connect.amazon.core.model.ARN
import se.ansman.kotshi.JsonSerializable
import java.time.Instant

@JsonSerializable
data class CompositeAlarm(
    val ActionsEnabled: Boolean? = null,
    val ActionsSuppressedBy: ActionsSuppressedBy? = null,
    val ActionsSuppressedReason: String? = null,
    val ActionsSuppressor: String? = null,
    val ActionsSuppressorExtensionPeriod: Int? = null,
    val ActionsSuppressorWaitPeriod: Int? = null,
    val AlarmActions: List<ARN>? = null,
    val AlarmArn: ARN? = null,
    val AlarmConfigurationUpdatedTimestamp: Instant? = null,
    val AlarmDescription: String? = null,
    val AlarmName: AlarmName? = null,
    val AlarmRule: String? = null,
    val InsufficientDataActions: List<ARN>? = null,
    val OKActions: List<ARN>? = null,
    val StateReason: String? = null,
    val StateReasonData: String? = null,
    val StateTransitionedTimestamp: Instant? = null,
    val StateUpdatedTimestamp: Instant? = null,
    val StateValue: AlarmState,
)
