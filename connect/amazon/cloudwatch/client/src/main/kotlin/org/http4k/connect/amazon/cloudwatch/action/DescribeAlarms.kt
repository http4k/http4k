package org.http4k.connect.amazon.cloudwatch.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.cloudwatch.CloudWatchAction
import org.http4k.connect.amazon.cloudwatch.model.AlarmName
import org.http4k.connect.amazon.cloudwatch.model.AlarmState
import org.http4k.connect.amazon.cloudwatch.model.AlarmType
import org.http4k.connect.amazon.cloudwatch.model.CompositeAlarm
import org.http4k.connect.amazon.cloudwatch.model.MetricAlarm
import org.http4k.connect.amazon.cloudwatch.model.NextToken
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
@JsonSerializable
data class DescribeAlarms(
    val AlarmNames: List<AlarmName>? = null,
    val AlarmNamePrefix: AlarmName? = null,
    val AlarmTypes: List<AlarmType>? = null,
    val ChildrenOfAlarmName: AlarmName? = null,
    val ParentsOfAlarmName: AlarmName? = null,
    val StateValue: AlarmState? = null,
    val ActionPrefix: String? = null,
    val MaxRecords: Int? = null,
    val NextToken: NextToken? = null,
) : CloudWatchAction<AlarmsDescribed>(AlarmsDescribed::class)

@JsonSerializable
data class AlarmsDescribed(
    val CompositeAlarms: List<CompositeAlarm>? = null,
    val MetricAlarms: List<MetricAlarm>? = null,
    val NextToken: NextToken? = null,
)
