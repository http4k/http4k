package org.http4k.connect.amazon.cloudwatch.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.cloudwatch.CloudWatchAction
import org.http4k.connect.amazon.cloudwatch.model.AlarmName
import org.http4k.connect.amazon.cloudwatch.model.ComparisonOperator
import org.http4k.connect.amazon.cloudwatch.model.Dimension
import org.http4k.connect.amazon.cloudwatch.model.EvaluateLowSampleCountPercentile
import org.http4k.connect.amazon.cloudwatch.model.ExtendedStatistic
import org.http4k.connect.amazon.cloudwatch.model.MetricDataQuery
import org.http4k.connect.amazon.cloudwatch.model.MetricName
import org.http4k.connect.amazon.cloudwatch.model.MetricUnit
import org.http4k.connect.amazon.cloudwatch.model.Namespace
import org.http4k.connect.amazon.cloudwatch.model.Statistic
import org.http4k.connect.amazon.cloudwatch.model.TreatMissingData
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.core.model.Tag
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
@JsonSerializable
data class PutMetricAlarm(
    val AlarmName: AlarmName,
    val ComparisonOperator: ComparisonOperator,
    val EvaluationPeriods: Int,
    val ActionsEnabled: Boolean? = null,
    val AlarmActions: List<ARN>? = null,
    val AlarmDescription: String? = null,
    val DatapointsToAlarm: Int? = null,
    val Dimensions: List<Dimension>? = null,
    val EvaluateLowSampleCountPercentile: EvaluateLowSampleCountPercentile? = null,
    val ExtendedStatistic: ExtendedStatistic? = null,
    val InsufficientDataActions: List<ARN>? = null,
    val MetricName: MetricName? = null,
    val Metrics: List<MetricDataQuery>? = null,
    val Namespace: Namespace? = null,
    val OKActions: List<ARN>? = null,
    val Period: Int? = null,
    val Statistic: Statistic? = null,
    val Tags: List<Tag>? = null,
    val Threshold: Double? = null,
    val ThresholdMetricId: String? = null,
    val TreatMissingData: TreatMissingData? = null,
    val Unit: MetricUnit? = null,
) : CloudWatchAction<Unit>(kotlin.Unit::class)

