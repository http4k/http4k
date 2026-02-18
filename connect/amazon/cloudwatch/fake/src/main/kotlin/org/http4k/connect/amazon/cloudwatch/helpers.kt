package org.http4k.connect.amazon.cloudwatch

import org.http4k.connect.amazon.cloudwatch.action.PutCompositeAlarm
import org.http4k.connect.amazon.cloudwatch.action.PutMetricAlarm
import org.http4k.connect.amazon.cloudwatch.action.SetAlarmState
import org.http4k.connect.amazon.cloudwatch.model.ActionsSuppressedBy
import org.http4k.connect.amazon.cloudwatch.model.AlarmState
import org.http4k.connect.amazon.cloudwatch.model.AlarmType
import org.http4k.connect.amazon.cloudwatch.model.CompositeAlarm
import org.http4k.connect.amazon.cloudwatch.model.DataPoint
import org.http4k.connect.amazon.cloudwatch.model.Dimension
import org.http4k.connect.amazon.cloudwatch.model.DimensionFilter
import org.http4k.connect.amazon.cloudwatch.model.ExtendedStatistic
import org.http4k.connect.amazon.cloudwatch.model.Metric
import org.http4k.connect.amazon.cloudwatch.model.MetricAlarm
import org.http4k.connect.amazon.cloudwatch.model.MetricDataResult
import org.http4k.connect.amazon.cloudwatch.model.MetricDatum
import org.http4k.connect.amazon.cloudwatch.model.MetricName
import org.http4k.connect.amazon.cloudwatch.model.MetricUnit
import org.http4k.connect.amazon.cloudwatch.model.Namespace
import org.http4k.connect.amazon.cloudwatch.model.PercentileExtendedStatistic
import org.http4k.connect.amazon.cloudwatch.model.ScanBy
import org.http4k.connect.amazon.cloudwatch.model.Statistic
import org.http4k.connect.amazon.cloudwatch.model.StatusCode
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.core.model.AwsAccount
import org.http4k.connect.amazon.core.model.Region
import org.http4k.connect.amazon.core.model.Tag
import java.time.Instant
import java.util.Collections.nCopies

fun Set<String>.filterKeys(keys: List<String>?) = if (keys == null) this else this intersect keys.toSet()

fun List<Alarm>.filterAlarmsByActionPrefix(actionPrefix: String?) = if (actionPrefix == null) this else
    filter {
        it.OKActions.orEmpty().any { it.value.startsWith(actionPrefix) } ||
            it.AlarmActions.orEmpty().any { it.value.startsWith(actionPrefix) } ||
            it.InsufficientDataActions.orEmpty().any { it.value.startsWith(actionPrefix) }
    }

fun List<Alarm>.filterAlarmsByAlarmTypes(alarmTypes: List<AlarmType>?) = if (alarmTypes == null) this else
    filter { it.AlarmType in alarmTypes }

fun List<Alarm>.filterAlarmsByStateValue(stateValue: AlarmState?) = if (stateValue == null) this else
    filter { it.State == stateValue }

fun List<Alarm>.filterAlarmsByMetricName(metricName: MetricName) = filter { it.MetricName == metricName }

fun List<Alarm>.filterAlarmsByNamespace(namespace: Namespace) = filter { it.Namespace == namespace }

fun List<Alarm>.filterAlarmsByDimensions(dimensions: Set<Dimension>?) =
    filter { it.Dimensions?.toSet() == dimensions }

fun List<Alarm>.filterAlarmsByStatistic(statistic: Statistic?) = if (statistic == null) this else
    filter { it.Statistic == statistic }

fun List<Alarm>.filterAlarmsByPercentileExtendedStatistic(extendedStatistic: PercentileExtendedStatistic?) =
    if (extendedStatistic == null) this
    else filter { it.ExtendedStatistic?.toPercentileExtendedStatisticOrNull() == extendedStatistic }

fun List<Alarm>.filterAlarmsByPeriod(period: Int?) = if (period == null) this else
    filter { it.Period == period }

fun List<Alarm>.filterAlarmsByUnit(unit: MetricUnit?) = if (unit == null) this else
    filter { it.Unit == unit }

fun Alarm.disabled() = copy(ActionsEnabled = false)

fun Alarm.enabled() = copy(ActionsEnabled = true)

fun Alarm.toCompositeAlarm(): CompositeAlarm {
    val ActionsSuppressedBy = when {
        ActionsSuppressor == null -> null
        ActionsSuppressorExtensionPeriod != null -> ActionsSuppressedBy.ExtensionPeriod
        ActionsSuppressorWaitPeriod != null -> ActionsSuppressedBy.WaitPeriod
        State == AlarmState.ALARM -> null
        else -> ActionsSuppressedBy.Alarm
    }
    return CompositeAlarm(
        ActionsEnabled = ActionsEnabled,
        ActionsSuppressedBy = ActionsSuppressedBy,
        ActionsSuppressedReason = if (ActionsSuppressedBy != null) {
            "$ActionsSuppressor: $ActionsSuppressedBy"
        } else null,
        ActionsSuppressor = ActionsSuppressor,
        ActionsSuppressorExtensionPeriod = ActionsSuppressorExtensionPeriod,
        ActionsSuppressorWaitPeriod = ActionsSuppressorWaitPeriod,
        AlarmActions = AlarmActions,
        AlarmArn = AlarmArn,
        AlarmConfigurationUpdatedTimestamp = LastUpdate,
        AlarmDescription = AlarmDescription,
        AlarmName = AlarmName,
        AlarmRule = AlarmRule,
        InsufficientDataActions = InsufficientDataActions,
        OKActions = OKActions,
        StateReason = StateReason,
        StateReasonData = StateReasonData,
        StateTransitionedTimestamp = LastStateTransitionTimestamp,
        StateUpdatedTimestamp = LastStateUpdateTimestamp,
        StateValue = State,
    )
}

fun PutCompositeAlarm.toAlarm(previous: Alarm?, region: Region, awsAccount: AwsAccount, now: Instant) = Alarm(
    AlarmName = AlarmName,
    AlarmArn = ARN.of(CloudWatch.awsService, region, awsAccount, AlarmName),
    ComparisonOperator = null,
    EvaluationPeriods = null,
    ActionsEnabled = ActionsEnabled,
    AlarmActions = AlarmActions,
    AlarmDescription = AlarmDescription,
    DatapointsToAlarm = null,
    Dimensions = null,
    EvaluateLowSampleCountPercentile = null,
    ExtendedStatistic = null,
    InsufficientDataActions = InsufficientDataActions,
    MetricName = null,
    Metrics = null,
    Namespace = null,
    OKActions = OKActions,
    Period = null,
    Statistic = null,
    Tags = Tags,
    Threshold = null,
    ThresholdMetricId = null,
    TreatMissingData = null,
    Unit = null,
    AlarmRule = AlarmRule,
    ActionsSuppressor = ActionsSuppressor,
    ActionsSuppressorExtensionPeriod = ActionsSuppressorExtensionPeriod,
    ActionsSuppressorWaitPeriod = ActionsSuppressorWaitPeriod,
    AlarmType = AlarmType.COMPOSITE_ALARM,
    LastUpdate = now,
    LastStateTransitionTimestamp = previous?.LastStateTransitionTimestamp ?: now,
    LastStateUpdateTimestamp = previous?.LastStateUpdateTimestamp ?: now,
    State = previous?.State ?: AlarmState.INSUFFICIENT_DATA,
    StateReason = previous?.StateReason,
    StateReasonData = previous?.StateReasonData,
)

fun Alarm.toMetricAlarm(): MetricAlarm {
    return MetricAlarm(
        AlarmName = AlarmName,
        AlarmArn = AlarmArn,
        ComparisonOperator = ComparisonOperator!!,
        ActionsEnabled = ActionsEnabled,
        AlarmActions = AlarmActions,
        AlarmConfigurationUpdatedTimestamp = LastUpdate,
        AlarmDescription = AlarmDescription,
        DataPointsToAlarm = DatapointsToAlarm,
        Dimensions = Dimensions,
        EvaluateLowSampleCountPercentile = EvaluateLowSampleCountPercentile,
        EvaluationPeriods = EvaluationPeriods,
        EvaluationState = null,
        ExtendedStatistic = ExtendedStatistic?.toPercentileExtendedStatisticOrNull(),
        InsufficientDataActions = InsufficientDataActions,
        MetricName = MetricName,
        Metrics = Metrics,
        Namespace = Namespace,
        OKActions = OKActions,
        Period = Period,
        StateReason = StateReason,
        StateReasonData = StateReasonData,
        StateTransitionedTimestamp = LastStateTransitionTimestamp,
        StateUpdatedTimestamp = LastStateUpdateTimestamp,
        StateValue = State,
        Statistic = Statistic,
        Threshold = Threshold,
        ThresholdMetricId = ThresholdMetricId,
        TreatMissingData = TreatMissingData,
        Unit = Unit,
    )
}

fun ExtendedStatistic.toPercentileExtendedStatisticOrNull() = try {
    PercentileExtendedStatistic.of(value)
} catch (e: Exception) {
    null
}

fun PutMetricAlarm.toAlarm(previous: Alarm?, region: Region, awsAccount: AwsAccount, now: Instant) =  Alarm(
    AlarmName = AlarmName,
    AlarmArn = ARN.of(CloudWatch.awsService, region, awsAccount, AlarmName),
    ComparisonOperator = ComparisonOperator,
    EvaluationPeriods = EvaluationPeriods,
    ActionsEnabled = ActionsEnabled,
    AlarmActions = AlarmActions,
    AlarmDescription = AlarmDescription,
    DatapointsToAlarm = DatapointsToAlarm,
    Dimensions = Dimensions,
    EvaluateLowSampleCountPercentile = EvaluateLowSampleCountPercentile,
    ExtendedStatistic = ExtendedStatistic,
    InsufficientDataActions = InsufficientDataActions,
    MetricName = MetricName,
    Metrics = Metrics,
    Namespace = Namespace,
    OKActions = OKActions,
    Period = Period,
    Statistic = Statistic,
    Tags = Tags,
    Threshold = Threshold,
    ThresholdMetricId = ThresholdMetricId,
    TreatMissingData = TreatMissingData,
    Unit = Unit,
    AlarmRule = null,
    ActionsSuppressor = null,
    ActionsSuppressorExtensionPeriod = null,
    ActionsSuppressorWaitPeriod = null,
    AlarmType = AlarmType.METRIC_ALARM,
    LastUpdate = now,
    LastStateTransitionTimestamp = previous?.LastStateTransitionTimestamp ?: now,
    LastStateUpdateTimestamp = previous?.LastStateUpdateTimestamp ?: now,
    State = previous?.State ?: AlarmState.INSUFFICIENT_DATA,
    StateReason = previous?.StateReason,
    StateReasonData = previous?.StateReasonData,
)

fun Alarm.setAlarmState(setAlarmState: SetAlarmState, now: Instant) = copy(
    State = setAlarmState.StateValue,
    StateReason = setAlarmState.StateReason,
    StateReasonData = setAlarmState.StateReasonData,
    LastStateUpdateTimestamp = now,
    LastStateTransitionTimestamp = if (setAlarmState.StateValue == State) LastStateTransitionTimestamp else now,
)

fun MetricDatum.toMetricDataResult() = MetricDataResult(
    Id = MetricName.value,
    Label = null,
    Messages = null,
    StatusCode = StatusCode.Complete,
    Timestamps = Values?.let { nCopies(it.size, Timestamp!!) },
    Values = Values,
)

fun MetricDatum.toDataPoint(): DataPoint {
    val valuesCountMap = Values?.let {
        it zip (Counts ?: nCopies(it.size, 1.0))
    }?.toMap()?.takeIf { it.isNotEmpty() }
    return DataPoint(
        Average = valuesCountMap?.let { it.entries.sumOf { it.key * it.value } / it.size },
        ExtendedStatistics = null,
        Maximum = valuesCountMap?.keys?.max(),
        Minimum = valuesCountMap?.keys?.min(),
        SampleCount = valuesCountMap?.values?.sum(),
        Sum = valuesCountMap?.let { it.entries.sumOf { it.key * it.value } },
        Timestamp = Timestamp,
        Unit = Unit,
    )
}

fun List<MetricDatum>.filterMetricDataByDimensions(filters: List<DimensionFilter>?) = if (filters == null) this else
    filter { metric -> metric.Dimensions != null && filters.all { filter -> metric.Dimensions!!.any { it matches filter } } }

infix fun Dimension.matches(filter: DimensionFilter) =
    Name == filter.Name && (filter.Value == null || filter.Value == Value)

fun List<MetricDatum>.filterMetricDataByMetricName(metricName: MetricName?) = if (metricName == null) this else
    filter { it.MetricName == metricName }

fun List<MetricDatum>.filterMetricDataByUnit(unit: MetricUnit?) = if (unit == null) this else
    filter { it.Unit == unit }

fun List<MetricDatum>.sortedMetricDataByScanBy(scanBy: ScanBy?) = when (scanBy) {
    null, ScanBy.TimestampDescending -> sortedByDescending { it.Timestamp!! }
    ScanBy.TimestampAscending -> sortedBy { it.Timestamp!!}
}

fun MetricDatum.toMetric(namespace: Namespace): Metric {
    return Metric(
        MetricName = MetricName,
        Namespace = namespace,
        Dimensions = Dimensions,
    )
}

fun MetricDatum.with(other: MetricDatum, now: Instant): MetricDatum {
    val zippedValues = Values?.let { values ->
        (values zip (Counts ?: nCopies(values.size, 1.0))).toMap().toMutableMap()
    }
    val otherZippedValues = other.Values?.let { values ->
        (values zip (other.Counts ?: nCopies(values.size, 1.0))).toMap()
    }
    val (newCounts, newValues) = if (otherZippedValues == null) {
        zippedValues?.keys?.toList() to zippedValues?.values?.toList()
    } else if (zippedValues == null) {
        otherZippedValues.keys.toList() to otherZippedValues.values.toList()
    } else {
        otherZippedValues.forEach { (key, value) ->
            zippedValues[key] = (zippedValues[key] ?: 0.0) + value
        }
        zippedValues.keys.toList() to zippedValues.values.toList()
    }
    val newDimensions = Dimensions?.associate { it.Name to it.Value }.orEmpty() +
        other.Dimensions?.associate { it.Name to it.Value }.orEmpty()
    return copy(
        Counts = newCounts,
        Dimensions = newDimensions.takeIf { it.isNotEmpty() }?.entries?.map { Dimension(it.key, it.value) },
        Timestamp = now,
        Value = other.Value,
        Values = newValues,
    )
}

fun Alarm.withTags(tags: List<Tag>, now: Instant): Alarm {
    val newTagsMap = Tags.orEmpty().associate { it.Key to it.Value } + tags.associate { it.Key to it.Value }
    val newTags = newTagsMap.entries.map { Tag(it.key, it.value) }
    return copy(
        Tags = newTags,
        LastUpdate = now
    )
}

fun Alarm.withoutTags(tagKeys: Set<String>, now: Instant): Alarm {
    return copy(
        Tags = Tags?.filter { it.Key !in tagKeys }?.takeIf { it.isNotEmpty() },
        LastUpdate = now
    )
}
