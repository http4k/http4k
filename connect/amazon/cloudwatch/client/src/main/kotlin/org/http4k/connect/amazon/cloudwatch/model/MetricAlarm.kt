package org.http4k.connect.amazon.cloudwatch.model

import org.http4k.connect.amazon.core.model.ARN
import se.ansman.kotshi.JsonSerializable
import java.time.Instant

@JsonSerializable
data class MetricAlarm(
    val AlarmName: AlarmName,
    val AlarmArn: ARN,
    val ComparisonOperator: ComparisonOperator,
    val ActionsEnabled: Boolean? = null,
    val AlarmActions: List<ARN>? = null,
    val AlarmConfigurationUpdatedTimestamp: Instant? = null,
    val AlarmDescription: String? = null,
    val DataPointsToAlarm: Int? = null,
    val Dimensions: List<Dimension>? = null,
    val EvaluateLowSampleCountPercentile: EvaluateLowSampleCountPercentile? = null,
    val EvaluationPeriods: Int? = null,
    val EvaluationState: EvaluationState? = null,
    val ExtendedStatistic: PercentileExtendedStatistic? = null,
    val InsufficientDataActions: List<ARN>? = null,
    val MetricName: MetricName? = null,
    val Metrics: List<MetricDataQuery>? = null,
    val Namespace: Namespace? = null,
    val OKActions: List<ARN>? = null,
    val Period: Int? = null,
    val StateReason: String? = null,
    val StateReasonData: String? = null,
    val StateTransitionedTimestamp: Instant? = null,
    val StateUpdatedTimestamp: Instant? = null,
    val StateValue: AlarmState,
    val Statistic: Statistic? = null,
    val Threshold: Double? = null,
    val ThresholdMetricId: String? = null,
    val TreatMissingData: TreatMissingData? = null,
    val Unit: MetricUnit? = null,
)
