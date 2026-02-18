package org.http4k.connect.amazon.cloudwatch

import org.http4k.aws.AwsCredentials
import org.http4k.chaos.ChaoticHttpHandler
import org.http4k.chaos.start
import org.http4k.connect.amazon.AwsJsonFake
import org.http4k.connect.amazon.cloudwatch.model.AlarmName
import org.http4k.connect.amazon.cloudwatch.model.AlarmState
import org.http4k.connect.amazon.cloudwatch.model.AlarmType
import org.http4k.connect.amazon.cloudwatch.model.ComparisonOperator
import org.http4k.connect.amazon.cloudwatch.model.Dimension
import org.http4k.connect.amazon.cloudwatch.model.EvaluateLowSampleCountPercentile
import org.http4k.connect.amazon.cloudwatch.model.ExtendedStatistic
import org.http4k.connect.amazon.cloudwatch.model.MetricDataQuery
import org.http4k.connect.amazon.cloudwatch.model.MetricDatum
import org.http4k.connect.amazon.cloudwatch.model.MetricName
import org.http4k.connect.amazon.cloudwatch.model.MetricUnit
import org.http4k.connect.amazon.cloudwatch.model.Namespace
import org.http4k.connect.amazon.cloudwatch.model.Statistic
import org.http4k.connect.amazon.cloudwatch.model.TreatMissingData
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.core.model.AwsAccount
import org.http4k.connect.amazon.core.model.AwsService
import org.http4k.connect.amazon.core.model.Region
import org.http4k.connect.amazon.core.model.Tag
import org.http4k.connect.storage.InMemory
import org.http4k.connect.storage.Storage
import org.http4k.core.Method.POST
import org.http4k.routing.bind
import org.http4k.routing.routes
import java.time.Clock
import java.time.Instant

data class Alarm(
    val AlarmName: AlarmName,
    val AlarmArn: ARN,
    val ComparisonOperator: ComparisonOperator?,
    val EvaluationPeriods: Int?,
    val ActionsEnabled: Boolean?,
    val AlarmActions: List<ARN>?,
    val AlarmDescription: String?,
    val DatapointsToAlarm: Int?,
    val Dimensions: List<Dimension>?,
    val EvaluateLowSampleCountPercentile: EvaluateLowSampleCountPercentile?,
    val ExtendedStatistic: ExtendedStatistic?,
    val InsufficientDataActions: List<ARN>?,
    val MetricName: MetricName?,
    val Metrics: List<MetricDataQuery>?,
    val Namespace: Namespace?,
    val OKActions: List<ARN>?,
    val Period: Int?,
    val Statistic: Statistic?,
    val Tags: List<Tag>?,
    val Threshold: Double?,
    val ThresholdMetricId: String?,
    val TreatMissingData: TreatMissingData?,
    val Unit: MetricUnit?,
    val AlarmRule: String?,
    val ActionsSuppressor: String?,
    val ActionsSuppressorExtensionPeriod: Int?,
    val ActionsSuppressorWaitPeriod: Int?,
    val AlarmType: AlarmType,
    val LastUpdate: Instant,
    val LastStateTransitionTimestamp: Instant,
    val LastStateUpdateTimestamp: Instant,
    val State: AlarmState,
    val StateReason: String?,
    val StateReasonData: String?,
)

class FakeCloudWatch(
    alarms: Storage<Alarm> = Storage.InMemory(),
    metrics: Storage<MutableList<MetricDatum>> = Storage.InMemory(),
    awsAccount: AwsAccount = AwsAccount.of("1234567890"),
    private val region: Region = Region.of("ldn-north-1"),
    clock: Clock = Clock.systemUTC()
) : ChaoticHttpHandler() {

    private val api = AwsJsonFake(CloudWatchMoshi, AwsService.of("GraniteServiceVersion20100801"))

    override val app = routes(
        "/" bind POST to routes(
            api.deleteAlarms(alarms),
            api.describeAlarms(alarms),
            api.describeAlarmsForMetric(alarms),
            api.disableAlarmActions(alarms),
            api.enableAlarmActions(alarms),
            api.getMetricData(metrics),
            api.getMetricStatistics(metrics),
            api.listMetrics(metrics),
            api.listTagsForResource(alarms),
            api.putCompositeAlarm(alarms, region, awsAccount, clock),
            api.putMetricAlarm(alarms, region, awsAccount, clock),
            api.putMetricData(metrics, clock),
            api.setAlarmState(alarms, clock),
            api.tagResource(alarms, clock),
            api.untagResource(alarms, clock),
        )
    )

    /**
     * Convenience function to get a CloudWatch client
     */
    fun client() = CloudWatch.Http(region, { AwsCredentials("accessKey", "secret") }, this)
}


fun main() {
    FakeCloudWatch().start()
}
