package org.http4k.connect.amazon.cloudwatch.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.cloudwatch.CloudWatchAction
import org.http4k.connect.amazon.cloudwatch.model.Dimension
import org.http4k.connect.amazon.cloudwatch.model.PercentileExtendedStatistic
import org.http4k.connect.amazon.cloudwatch.model.MetricAlarm
import org.http4k.connect.amazon.cloudwatch.model.MetricName
import org.http4k.connect.amazon.cloudwatch.model.MetricUnit
import org.http4k.connect.amazon.cloudwatch.model.Namespace
import org.http4k.connect.amazon.cloudwatch.model.Statistic
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
@JsonSerializable
data class DescribeAlarmsForMetric(
    val MetricName: MetricName,
    val Namespace: Namespace,
    val Dimensions: List<Dimension>? = null,
    val ExtendedStatistic: PercentileExtendedStatistic? = null,
    val Period: Int? = null,
    val Statistic: Statistic? = null,
    val Unit: MetricUnit? = null,
) : CloudWatchAction<AlarmsDescribedForMetric>(AlarmsDescribedForMetric::class)

@JsonSerializable
data class AlarmsDescribedForMetric(
    val MetricAlarms: List<MetricAlarm>,
)
