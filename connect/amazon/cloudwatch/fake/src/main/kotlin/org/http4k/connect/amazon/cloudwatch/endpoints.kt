package org.http4k.connect.amazon.cloudwatch

import org.http4k.connect.amazon.AwsJsonFake
import org.http4k.connect.amazon.JsonError
import org.http4k.connect.amazon.cloudwatch.action.AlarmsDescribed
import org.http4k.connect.amazon.cloudwatch.action.AlarmsDescribedForMetric
import org.http4k.connect.amazon.cloudwatch.action.DeleteAlarms
import org.http4k.connect.amazon.cloudwatch.action.DescribeAlarms
import org.http4k.connect.amazon.cloudwatch.action.DescribeAlarmsForMetric
import org.http4k.connect.amazon.cloudwatch.action.DisableAlarmActions
import org.http4k.connect.amazon.cloudwatch.action.EnableAlarmActions
import org.http4k.connect.amazon.cloudwatch.action.GetMetricData
import org.http4k.connect.amazon.cloudwatch.action.GetMetricStatistics
import org.http4k.connect.amazon.cloudwatch.action.ListMetrics
import org.http4k.connect.amazon.cloudwatch.action.ListTagsForResource
import org.http4k.connect.amazon.cloudwatch.action.ListedTags
import org.http4k.connect.amazon.cloudwatch.action.MetricData
import org.http4k.connect.amazon.cloudwatch.action.MetricStatistics
import org.http4k.connect.amazon.cloudwatch.action.Metrics
import org.http4k.connect.amazon.cloudwatch.action.PutCompositeAlarm
import org.http4k.connect.amazon.cloudwatch.action.PutMetricAlarm
import org.http4k.connect.amazon.cloudwatch.action.PutMetricData
import org.http4k.connect.amazon.cloudwatch.action.SetAlarmState
import org.http4k.connect.amazon.cloudwatch.action.TagResource
import org.http4k.connect.amazon.cloudwatch.action.UntagResource
import org.http4k.connect.amazon.cloudwatch.model.AlarmType
import org.http4k.connect.amazon.cloudwatch.model.MetricDatum
import org.http4k.connect.amazon.cloudwatch.model.Namespace
import org.http4k.connect.amazon.cloudwatch.model.NextToken
import org.http4k.connect.amazon.core.model.AwsAccount
import org.http4k.connect.amazon.core.model.Region
import org.http4k.connect.storage.Storage
import org.http4k.connect.storage.getOrPut
import java.time.Clock
import kotlin.math.pow

fun AwsJsonFake.deleteAlarms(alarms: Storage<Alarm>) = route<DeleteAlarms> {
    val alarmNames = alarms.keySet("")
    val missingAlarms = it.AlarmNames.map { it.value }.filter { it !in alarmNames }
    if (missingAlarms.isNotEmpty()) {
        return@route JsonError("Invalid parameter value", "Some specified alarms do not exist: $missingAlarms")
    }
    it.AlarmNames.forEach { alarms -= it.value }
}

fun AwsJsonFake.describeAlarms(alarms: Storage<Alarm>) = route<DescribeAlarms> {
    if (it.AlarmNames != null && it.AlarmNamePrefix != null) {
        return@route JsonError("Invalid parameter value", "You cannot specify both AlarmNames and AlarmNamePrefix")
    }
    if (it.AlarmNames != null && it.AlarmNames!!.isEmpty()) {
        return@route JsonError("Invalid parameter value", "AlarmNames must not be present or not empty")
    }
    if (it.AlarmNames != null && it.AlarmNames!!.size > 100) {
        return@route JsonError("Invalid parameter value", "You must not provide more than 100 alarm names")
    }
    if (it.MaxRecords != null && it.MaxRecords!! !in 1..100) {
        return@route JsonError("Invalid parameter value", "MaxRecords must not be specified or lie between 1 and 100. Was ${it.MaxRecords}")
    }
    if (it.ActionPrefix != null && it.ActionPrefix!!.isEmpty()) {
        return@route JsonError("Invalid parameter value", "ActionPrefix must not be specified or not be empty")
    }
    if (it.AlarmTypes != null && it.AlarmTypes!!.isEmpty()) {
        return@route JsonError("Invalid parameter value", "AlarmTypes must not be specified or not be empty")
    }
    val filteredAlarms = alarms.keySet(it.AlarmNamePrefix?.value.orEmpty())
        .filterKeys(it.AlarmNames?.map { it.value })
        .map { alarms[it]!! }
        .filterAlarmsByActionPrefix(it.ActionPrefix)
        .filterAlarmsByAlarmTypes(it.AlarmTypes)
        .filterAlarmsByStateValue(it.StateValue)
    val maxRecords = it.MaxRecords ?: 100
    val nextToken = try {
        it.NextToken?.value?.toInt()
    } catch (e: NumberFormatException) {
        return@route JsonError("invalid parameter value", "Invalid NextToken provided: ${it.NextToken}")
    }
    if (nextToken != null && (nextToken * maxRecords !in 1..<filteredAlarms.size)) {
        return@route JsonError("invalid parameter value", "Invalid NextToken provided: ${it.NextToken}")
    }
    val (metricAlarms, compositeAlarms) = when (nextToken) {
        null -> filteredAlarms.take(maxRecords)
        else -> filteredAlarms.drop(maxRecords * nextToken).take(maxRecords)
    }.partition { it.AlarmType == AlarmType.METRIC_ALARM }
    val nextNextToken = when {
        filteredAlarms.isEmpty() -> null
        filteredAlarms.last() == metricAlarms.lastOrNull() -> null
        filteredAlarms.last() == compositeAlarms.lastOrNull() -> null
        nextToken == null -> NextToken.of("1")
        else -> NextToken.of((nextToken + 1).toString())
    }
    AlarmsDescribed(
        CompositeAlarms = compositeAlarms.map { it.toCompositeAlarm() },
        MetricAlarms = metricAlarms.map { it.toMetricAlarm() },
        NextToken = nextNextToken,
    )
}

fun AwsJsonFake.describeAlarmsForMetric(alarms: Storage<Alarm>) = route<DescribeAlarmsForMetric> {
    if (it.Dimensions != null && it.Dimensions!!.size !in 1..30) {
        return@route JsonError("invalid parameter value", "Dimensions must not be specified or have between 1 and 30 elements. Found ${it.Dimensions!!.size} elements")
    }
    alarms.keySet("").mapNotNull { alarms[it] }
        .filterAlarmsByAlarmTypes(listOf(AlarmType.METRIC_ALARM))
        .filterAlarmsByMetricName(it.MetricName)
        .filterAlarmsByNamespace(it.Namespace)
        .filterAlarmsByDimensions(it.Dimensions?.toSet())
        .filterAlarmsByStatistic(it.Statistic)
        .filterAlarmsByPercentileExtendedStatistic(it.ExtendedStatistic)
        .filterAlarmsByPeriod(it.Period)
        .filterAlarmsByUnit(it.Unit)
        .map { it.toMetricAlarm() }
        .let(::AlarmsDescribedForMetric)
}

fun AwsJsonFake.disableAlarmActions(alarms: Storage<Alarm>) = route<DisableAlarmActions> {
    if (it.AlarmNames.size !in 1..100) {
        return@route JsonError("invalid parameter value", "Number of specified alarms must be between 1 and 100. Received ${it.AlarmNames.size}")
    }
    val alarmsToDisable = alarms.keySet("") intersect it.AlarmNames.map { it.value }.toSet()
    alarmsToDisable.mapNotNull { alarms[it] }.forEach {
        alarms[it.AlarmName.value] = it.disabled()
    }
}

fun AwsJsonFake.enableAlarmActions(alarms: Storage<Alarm>) = route<EnableAlarmActions> {
    if (it.AlarmNames.size !in 1..100) {
        return@route JsonError(
            "invalid parameter value",
            "Number of specified alarms must be between 1 and 100. Received ${it.AlarmNames.size}"
        )
    }
    (alarms.keySet("") intersect it.AlarmNames.map { it.value }.toSet())
        .mapNotNull { alarms[it] }
        .forEach {
            alarms[it.AlarmName.value] = it.enabled()
        }
}

fun AwsJsonFake.putCompositeAlarm(alarms: Storage<Alarm>, region: Region, awsAccount: AwsAccount, clock: Clock) = route<PutCompositeAlarm> {
    if (alarms.keySet().size >= 5000) {
        return@route JsonError(
            "limit exceeded",
            "Region can contain at most 5000 alarms, which has already been reached"
        )
    } else {
        alarms[it.AlarmName.value] = it.toAlarm(alarms[it.AlarmName.value], region, awsAccount, clock.instant())
    }
}

fun AwsJsonFake.putMetricAlarm(alarms: Storage<Alarm>, region: Region, awsAccount: AwsAccount, clock: Clock) = route<PutMetricAlarm> {
    if (alarms.keySet().size >= 5000) {
        return@route JsonError(
            "limit exceeded",
            "Region can contain at most 5000 alarms, which has already been reached"
        )
    } else {
        alarms[it.AlarmName.value] = it.toAlarm(alarms[it.AlarmName.value], region, awsAccount, clock.instant())
    }
}

fun AwsJsonFake.setAlarmState(alarms: Storage<Alarm>, clock: Clock) = route<SetAlarmState> {
    when (val alarm: Alarm? = alarms[it.AlarmName.value]) {
        null -> JsonError("not found", "${it.AlarmName} not found")
        else -> alarms[it.AlarmName.value] = alarm.setAlarmState(it, clock.instant())
    }
}

fun AwsJsonFake.getMetricData(metrics: Storage<MutableList<MetricDatum>>) = route<GetMetricData> {
    val maxDataPoints = when (val maxPoints = it.MaxDataPoints) {
        null -> 100800
        in 1..100800 -> maxPoints
        else -> return@route JsonError("invalid parameter value", "When specifying MaxDataPoints, it must lie between 1 and 100800. Found $maxPoints")
    }
    if (it.MetricDataQueries.size !in 1..500) {
        return@route JsonError("invalid parameter value", "You must provide at least 1 and at most 500 MetricDataQuery entries. Found ${it.MetricDataQueries.size}")
    }
    if (it.EndTime.isBefore(it.StartTime)) {
        return@route JsonError("invalid parameter value", "EndTime cannot be before StartTime")
    }
    val nextToken = try {
        it.NextToken?.value?.toInt()
    } catch (e: NumberFormatException) {
        return@route JsonError("invalid parameter value", "Invalid NextToken provided: ${it.NextToken}")
    }
    val metricData = metrics.keySet("").mapNotNull { metrics[it] }.flatten()
        .filter { metric -> metric.Timestamp!! in it.EndTime..it.StartTime }
        .sortedMetricDataByScanBy(it.ScanBy)
    if (nextToken != null && (maxDataPoints * nextToken !in 1..<metricData.size)) {
        return@route JsonError("invalid parameter value", "Invalid NextToken provided: ${it.NextToken}")
    }
    val metricDataToReturn = when (nextToken) {
        null -> metricData.take(maxDataPoints)
        else -> metricData.drop(nextToken * maxDataPoints).take(maxDataPoints)
    }
    val nextNextToken = when {
        metricData.isEmpty() -> null
        metricData.last() == metricDataToReturn.last() -> null
        nextToken == null -> NextToken.of("1")
        else -> NextToken.of((nextToken + 1).toString())
    }
    MetricData(
        Messages = emptyList(),
        MetricDataResults = metricDataToReturn.map { it.toMetricDataResult() },
        NextToken = nextNextToken
    )
}

fun AwsJsonFake.getMetricStatistics(metrics: Storage<MutableList<MetricDatum>>) = route<GetMetricStatistics> {
    if (it.Dimensions != null && it.Dimensions!!.size !in 1..30) {
        return@route JsonError("invalid parameter value", "When specifying dimensions, you must specify between 1 and 30 dimensions. Found ${it.Dimensions!!.size}")
    }
    val metricData = metrics[it.Namespace.value]?.filter { metric -> metric.MetricName == it.MetricName }
        ?.filterMetricDataByUnit(it.Unit)
        ?.filter { metric -> it.Dimensions == null || it.Dimensions!!.toSet() == metric.Dimensions!!.toSet() }
        ?.filter { metric -> metric.Timestamp!! in it.StartTime..<it.EndTime }
    MetricStatistics(
        Datapoints = metricData?.map { it.toDataPoint() }.orEmpty(),
        Label = null,
    )
}

fun AwsJsonFake.listMetrics(metrics: Storage<MutableList<MetricDatum>>) = route<ListMetrics> {
    if (it.Dimensions != null && it.Dimensions!!.size !in 1..10) {
        return@route JsonError("invalid parameter value", "When specifying dimensions, you must specify between 1 and 10 dimensions. Found ${it.Dimensions!!.size}")
    }
    val nextToken = try {
        it.NextToken?.value?.toInt()
    } catch (e: NumberFormatException) {
        return@route JsonError("invalid parameter value", "Invalid NextToken provided: ${it.NextToken}")
    }
    val metricData = if (it.Namespace != null) {
        metrics[it.Namespace!!.value].orEmpty().map { metric -> it.Namespace!! to metric }
    } else {
        metrics.keySet("").map { Namespace.of(it) to metrics[it] }
            .flatMap { (namespace, list) ->
                list.orEmpty().filterMetricDataByDimensions(it.Dimensions)
                    .filterMetricDataByMetricName(it.MetricName)
                    .map { namespace to it }
            }
    }
    if (nextToken != null && (500 * nextToken !in 1..<metricData.size)) {
        return@route JsonError("invalid parameter value", "Invalid NextToken provided: ${it.NextToken}")
    }
    val metricDataToReturn = when (nextToken) {
        null -> metricData.take(500)
        else -> metricData.drop(500 * nextToken).take(500)
    }
    val nextNextToken = when {
        metricData.isEmpty() -> null
        metricData.last() == metricDataToReturn.last() -> null
        nextToken == null -> NextToken.of("1")
        else -> NextToken.of((nextToken + 1).toString())
    }
    Metrics(
        Metrics = metricDataToReturn.map { (namespace, metricDatum) -> metricDatum.toMetric(namespace) },
        NextToken = nextNextToken,
        OwningAccounts = null,
    )
}

fun AwsJsonFake.putMetricData(metrics: Storage<MutableList<MetricDatum>>, clock: Clock) = route<PutMetricData> {
    if (it.EntityMetricData != null && it.StrictEntityValidation == null) {
        return@route JsonError("invalid parameter value", "When specifying EntityMetricData, you must also specify StrictEntityValidation")
    }
    if (it.EntityMetricData != null && it.EntityMetricData!!.size !in 1..2) {
        return@route JsonError("invalid parameter value", "When specifying EntityMetricData, you must specify at least 1 and at most 2 entries. Found ${it.EntityMetricData!!.size}")
    }
    if (it.MetricData != null && it.MetricData!!.isEmpty()) {
        return@route JsonError("invalid parameter value", "When specifying MetricData, you must specify at least 1 entry. Found None.")
    }
    val metricData = it.EntityMetricData.orEmpty()
        .flatMap { it.MetricData.orEmpty() }
        .plus(it.MetricData.orEmpty())
        .map { it.copy(Timestamp = it.Timestamp ?: clock.instant()) }
    if (metricData.size !in 1..1000) {
        return@route JsonError("invalid parameter value", "Total metric data size must be between one and 1000. Found ${metricData.size}")
    }
    val metricList = metrics.getOrPut(it.Namespace) { mutableListOf() }
    val boundary = 2.0.pow(360)
    for (metricDatum in metricData) {
        if (metricDatum.Counts != null && metricDatum.Values != null && metricDatum.Counts!!.size != metricDatum.Values!!.size) {
            return@route JsonError("invalid parameter value", "Counts and values for metric ${metricDatum.MetricName} don't have the same size")
        }
        if (metricDatum.Values != null && metricDatum.Values!!.any { it !in -boundary..boundary }) {
            return@route JsonError("invalid parameter value", "A value for metric ${metricDatum.MetricName} is outside the range -2^360..2^360")
        }
        if (metricDatum.Values != null && metricDatum.Values!!.distinct() != metricDatum.Values!!) {
            return@route JsonError("invalid parameter value", "Provided values for metric ${metricDatum.MetricName} must all be distinct")
        }
        if (metricDatum.Values != null && metricDatum.Values!!.size > 150) {
            return@route JsonError("invalid parameter value", "A metric can have at most 150 new values associated with it in a put request. Found ${metricDatum.Values!!.size} values for metric ${metricDatum.MetricName}")
        }
        val existingMetricWithIndex = metricList.withIndex().find { (_, m) -> m.MetricName == metricDatum.MetricName }
        when (existingMetricWithIndex) {
            null -> metricList.add(metricDatum)
            else -> {
                val (index, existingMetric) = existingMetricWithIndex
                metricList[index] = existingMetric.with(metricDatum, clock.instant())
            }
        }
    }
}

fun AwsJsonFake.listTagsForResource(alarms: Storage<Alarm>) = route<ListTagsForResource> {
    val alarm = alarms.keySet("").mapNotNull { alarms[it] }
        .find { alarm -> alarm.AlarmArn == it.ResourceARN }
    when (alarm) {
        null -> JsonError("not found", "${it.ResourceARN} not found")
        else -> ListedTags(alarm.Tags.orEmpty())
    }
}

fun AwsJsonFake.tagResource(alarms: Storage<Alarm>, clock: Clock) = route<TagResource> {
    if (it.Tags.size !in 1..50) {
        return@route JsonError("invalid parameter provided", "Invalid number of tags provided: ${it.Tags.size}. Must be between 1 and 50")
    }
    val alarmKey = alarms.keySet("").firstOrNull { key -> alarms[key]?.AlarmArn == it.ResourceARN }
    when (alarmKey) {
        null -> JsonError("not found", "${it.ResourceARN} not found")
        else -> {
            val updatedAlarm = alarms[alarmKey]!!.withTags(it.Tags, clock.instant())
            if (updatedAlarm.Tags!!.size > 50) return@route JsonError("internal error", "Resource ${it.ResourceARN} would have more than 50 tags")
            alarms[alarmKey] = updatedAlarm
        }
    }
}

fun AwsJsonFake.untagResource(alarms: Storage<Alarm>, clock: Clock) = route<UntagResource> {
    val alarmKey = alarms.keySet("").firstOrNull { key -> alarms[key]?.AlarmArn == it.ResourceARN }
    when (alarmKey) {
        null -> JsonError("not found", "${it.ResourceARN} not found")
        else -> alarms[alarmKey] = alarms[alarmKey]!!.withoutTags(it.TagKeys.toSet(), clock.instant())
    }
}
