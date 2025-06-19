package org.http4k.connect.amazon.cloudwatch.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.cloudwatch.CloudWatchAction
import org.http4k.connect.amazon.cloudwatch.model.LabelOptions
import org.http4k.connect.amazon.cloudwatch.model.MessageData
import org.http4k.connect.amazon.cloudwatch.model.MetricDataQuery
import org.http4k.connect.amazon.cloudwatch.model.MetricDataResult
import org.http4k.connect.amazon.cloudwatch.model.NextToken
import org.http4k.connect.amazon.cloudwatch.model.ScanBy
import se.ansman.kotshi.JsonSerializable
import java.time.Instant

@Http4kConnectAction
@JsonSerializable
data class GetMetricData(
    val MetricDataQueries: List<MetricDataQuery>,
    val StartTime: Instant,
    val EndTime: Instant,
    val NextToken: NextToken? = null,
    val ScanBy: ScanBy? = null,
    val MaxDataPoints: Int? = null,
    val LabelOptions: LabelOptions? = null,
) : CloudWatchAction<MetricData>(MetricData::class)

@JsonSerializable
data class MetricData(
    val Messages: List<MessageData>,
    val MetricDataResults: List<MetricDataResult>,
    val NextToken: NextToken? = null,
)
