package org.http4k.connect.amazon.cloudwatch.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.cloudwatch.CloudWatchAction
import org.http4k.connect.amazon.cloudwatch.model.DataPoint
import org.http4k.connect.amazon.cloudwatch.model.Dimension
import org.http4k.connect.amazon.cloudwatch.model.PercentileExtendedStatistic
import org.http4k.connect.amazon.cloudwatch.model.MetricName
import org.http4k.connect.amazon.cloudwatch.model.MetricUnit
import org.http4k.connect.amazon.cloudwatch.model.Namespace
import org.http4k.connect.amazon.cloudwatch.model.Statistic
import se.ansman.kotshi.JsonSerializable
import java.time.Instant

@Http4kConnectAction
@JsonSerializable
data class GetMetricStatistics(
    val MetricName: MetricName,
    val Namespace: Namespace,
    val StartTime: Instant,
    val EndTime: Instant,
    val Period: Int,
    val Dimensions: List<Dimension>? = null,
    val ExtendedStatistics: List<PercentileExtendedStatistic>? = null,
    val Statistics: List<Statistic>? = null,
    val Unit: MetricUnit? = null,
): CloudWatchAction<MetricStatistics>(MetricStatistics::class)

@JsonSerializable
data class MetricStatistics(
    val Datapoints: List<DataPoint>,
    val Label: String? = null,
)
