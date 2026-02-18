package org.http4k.connect.amazon.cloudwatch

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.greaterThan
import com.natpryce.hamkrest.hasSize
import com.natpryce.hamkrest.isEmpty
import org.http4k.connect.amazon.AwsContract
import org.http4k.connect.amazon.cloudwatch.model.AlarmName
import org.http4k.connect.amazon.cloudwatch.model.AlarmState
import org.http4k.connect.amazon.cloudwatch.model.AlarmType
import org.http4k.connect.amazon.cloudwatch.model.ComparisonOperator
import org.http4k.connect.amazon.cloudwatch.model.Metric
import org.http4k.connect.amazon.cloudwatch.model.MetricDataQuery
import org.http4k.connect.amazon.cloudwatch.model.MetricDatum
import org.http4k.connect.amazon.cloudwatch.model.MetricName
import org.http4k.connect.amazon.cloudwatch.model.MetricStat
import org.http4k.connect.amazon.cloudwatch.model.MetricUnit
import org.http4k.connect.amazon.cloudwatch.model.Namespace
import org.http4k.connect.amazon.cloudwatch.model.Statistic
import org.http4k.connect.amazon.core.model.Tag
import org.http4k.connect.successValue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import java.time.Instant

interface CloudWatchContract : AwsContract {
    val cloudWatch get() = CloudWatch.Http(aws.region, { aws.credentials }, http)

    @Test
    fun `metric alarm lifecycle`() {
        val alarmName = AlarmName.of("http4k-connect-test-alarm")
        cloudWatch.putMetricAlarm(
            AlarmName = alarmName,
            AlarmDescription = "Alarm for testing purposes",
            ActionsEnabled = false,
            ComparisonOperator = ComparisonOperator.GreaterThanThreshold,
            DatapointsToAlarm = 1,
            EvaluationPeriods = 1,
            Statistic = Statistic.Minimum,
            MetricName = MetricName.of("htt4k-connect-test-metric"),
            Namespace = Namespace.of("http4k-connect-test-alarms"),
            Period = 60,
            Threshold = 1.0,
        )
        try {
            val metricAlarms = cloudWatch.describeAlarms().successValue().MetricAlarms
            assertNotNull(metricAlarms)
            assertThat(metricAlarms, hasSize(equalTo(1)))
            assertThat(metricAlarms.first().AlarmName, equalTo(alarmName))
            assertThat(metricAlarms.first().ActionsEnabled, equalTo(false))
            cloudWatch.enableAlarmActions(
                AlarmNames = listOf(alarmName),
            )
            cloudWatch.setAlarmState(
                AlarmName = alarmName,
                StateValue = AlarmState.ALARM,
                StateReason = "Test alarm state ALARM",
            )
            val alarmsDescribedForMetric = cloudWatch.describeAlarmsForMetric(
                MetricName = MetricName.of("htt4k-connect-test-metric"),
                Namespace = Namespace.of("http4k-connect-test-alarms"),
            ).successValue().MetricAlarms
            assertThat(alarmsDescribedForMetric, hasSize(equalTo(1)))
            assertThat(alarmsDescribedForMetric.first().AlarmName, equalTo(alarmName))
            assertThat(alarmsDescribedForMetric.first().ActionsEnabled, equalTo(true))
            assertThat(alarmsDescribedForMetric.first().StateValue, equalTo(AlarmState.ALARM))
        } finally {
            cloudWatch.deleteAlarms(
                AlarmNames = listOf(alarmName),
            )
            assertThat(
                cloudWatch.describeAlarms(
                    AlarmTypes = listOf(AlarmType.METRIC_ALARM, AlarmType.COMPOSITE_ALARM)
                ).successValue().MetricAlarms.orEmpty(),
                isEmpty,
            )
        }
    }

    @Test
    fun `composite alarm lifecycle`() {
        val alarmName = AlarmName.of("http4k-connect-test-alarm")
        cloudWatch.putCompositeAlarm(
            AlarmName = alarmName,
            AlarmRule = "false",
            AlarmDescription = "Alarm for testing purposes, set to false",
            ActionsEnabled = true,
        )
        try {
            val compositeAlarms = cloudWatch.describeAlarms(
                AlarmTypes = listOf(AlarmType.COMPOSITE_ALARM)
            ).successValue().CompositeAlarms
            assertNotNull(compositeAlarms)
            assertThat(compositeAlarms, hasSize(equalTo(1)))
            assertThat(compositeAlarms.first().AlarmName, equalTo(alarmName))
            assertThat(compositeAlarms.first().ActionsEnabled, equalTo(true))
            val alarmsDescribedForMetric = cloudWatch.describeAlarmsForMetric(
                MetricName = MetricName.of("htt4k-connect-test-metric"),
                Namespace = Namespace.of("http4k-connect-test-alarms"),
            ).successValue().MetricAlarms
            assertThat(alarmsDescribedForMetric, hasSize(equalTo(0)))
            cloudWatch.disableAlarmActions(
                AlarmNames = listOf(alarmName),
            )
            val compositeAlarmsWithDisabledActions = cloudWatch.describeAlarms(
                AlarmTypes = listOf(AlarmType.COMPOSITE_ALARM)
            ).successValue().CompositeAlarms
            assertNotNull(compositeAlarmsWithDisabledActions)
            assertThat(compositeAlarmsWithDisabledActions, hasSize(equalTo(1)))
            assertThat(compositeAlarmsWithDisabledActions.first().AlarmName, equalTo(alarmName))
            assertThat(compositeAlarmsWithDisabledActions.first().ActionsEnabled, equalTo(false))
        } finally {
            cloudWatch.deleteAlarms(
                AlarmNames = listOf(alarmName),
            )
            assertThat(
                cloudWatch.describeAlarms(
                    AlarmTypes = listOf(AlarmType.METRIC_ALARM, AlarmType.COMPOSITE_ALARM)
                ).successValue().MetricAlarms.orEmpty(),
                isEmpty,
            )
        }
    }

    @Test
    fun `alarm tags lifecycle`() {
        val alarmName = AlarmName.of("http4k-connect-test-alarm")
        cloudWatch.putMetricAlarm(
            AlarmName = alarmName,
            AlarmDescription = "Alarm for testing purposes",
            ComparisonOperator = ComparisonOperator.GreaterThanThreshold,
            DatapointsToAlarm = 1,
            EvaluationPeriods = 1,
            Statistic = Statistic.Minimum,
            MetricName = MetricName.of("htt4k-connect-test-metric"),
            Namespace = Namespace.of("http4k-connect-test-alarms"),
            Period = 60,
            Threshold = 1.0,
        )
        try {
            val metricAlarms = cloudWatch.describeAlarms(AlarmNames = listOf(alarmName)).successValue().MetricAlarms
            assertNotNull(metricAlarms)
            val metricAlarm = metricAlarms.first()
            val alarmArn = metricAlarm.AlarmArn
            cloudWatch.tagResource(
                ResourceARN = alarmArn,
                Tags = listOf(
                    Tag("http4k-connect-test-tag-key-1", "http4k-connect-test-tag-value-1"),
                    Tag("http4k-connect-test-tag-key-2", "http4k-connect-test-tag-value-2"),
                )
            )
            val listedTags = cloudWatch.listTagsForResource(
                ResourceARN = alarmArn,
            ).successValue()
            assertThat(
                listedTags.Tags, equalTo(
                    listOf(
                        Tag("http4k-connect-test-tag-key-1", "http4k-connect-test-tag-value-1"),
                        Tag("http4k-connect-test-tag-key-2", "http4k-connect-test-tag-value-2"),
                    )
                )
            )
            cloudWatch.untagResource(
                ResourceARN = alarmArn,
                TagKeys = listOf("http4k-connect-test-tag-key-1")
            )
            val reducedListedTags = cloudWatch.listTagsForResource(
                ResourceARN = alarmArn,
            ).successValue()
            assertThat(
                reducedListedTags.Tags, equalTo(
                    listOf(
                        Tag("http4k-connect-test-tag-key-2", "http4k-connect-test-tag-value-2"),
                    )
                )
            )
        } finally {
            cloudWatch.deleteAlarms(
                AlarmNames = listOf(alarmName),
            )
            assertThat(
                cloudWatch.describeAlarms(
                    AlarmTypes = listOf(AlarmType.METRIC_ALARM, AlarmType.COMPOSITE_ALARM)
                ).successValue().MetricAlarms.orEmpty(),
                isEmpty,
            )
        }
    }

    @Test
    fun `metric data lifecycle`() {
        val namespace = Namespace.of("http4k-connect-test-namespace")
        val metricName = MetricName.of("http4k-connect-test-metric-name")
        val timestamp = Instant.now()
        cloudWatch.putMetricData(
            Namespace = namespace,
            EntityMetricData = null,
            MetricData = listOf(
                MetricDatum(
                    MetricName = metricName,
                    Timestamp = timestamp,
                    Unit = MetricUnit.Count_per_Second,
                    Value = 1.0,
                    Values = listOf(0.5, 1.0),
                    StorageResolution = 60,
                ),
            ),
            StrictEntityValidation = null,
        )
        cloudWatch.getMetricData(
            MetricDataQueries = listOf(
                MetricDataQuery(
                    Id = "http4k_connect_test_metric_data_query_id",
                    MetricStat = MetricStat(
                        Metric = Metric(
                            MetricName = metricName,
                            Namespace = namespace,
                        ),
                        Stat = "Maximum",
                        Unit = MetricUnit.Count_per_Second,
                    )
                )
            ),
            StartTime = timestamp.minusSeconds(120),
            EndTime = timestamp.plusSeconds(60),
        ).successValue()
        val metricsList = cloudWatch.listMetrics(
            MetricName = metricName,
            Namespace = namespace,
        ).successValue()
        assertThat(metricsList.Metrics, hasSize(equalTo(1)))
        val metric = metricsList.Metrics.first()
        assertThat(metric.MetricName, equalTo(metricName))
        assertThat(metric.Namespace, equalTo(namespace))
        val metricStatistics = cloudWatch.getMetricStatistics(
            MetricName = metricName,
            Namespace = namespace,
            StartTime = timestamp.minusSeconds(120),
            EndTime = timestamp.plusSeconds(60),
            Period = 60,
            Unit = MetricUnit.Count_per_Second,
        ).successValue()
        assertThat(metricStatistics.Datapoints, hasSize(greaterThan(0)))
        assertThat(metricStatistics.Datapoints.first().Maximum, equalTo(1.0))
    }
}
