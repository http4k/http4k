package org.http4k.connect.amazon.cloudwatch.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.cloudwatch.CloudWatchAction
import org.http4k.connect.amazon.cloudwatch.model.DimensionFilter
import org.http4k.connect.amazon.cloudwatch.model.Metric
import org.http4k.connect.amazon.cloudwatch.model.MetricName
import org.http4k.connect.amazon.cloudwatch.model.Namespace
import org.http4k.connect.amazon.cloudwatch.model.NextToken
import org.http4k.connect.amazon.cloudwatch.model.RecentlyActive
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
@JsonSerializable
data class ListMetrics(
    val Dimensions: List<DimensionFilter>? = null,
    val IncludeLinkedAccounts: Boolean? = null,
    val MetricName: MetricName? = null,
    val Namespace: Namespace? = null,
    val NextToken: NextToken? = null,
    val OwningAccount: String? = null,
    val RecentlyActive: RecentlyActive? = null,
): CloudWatchAction<Metrics>(Metrics::class)

@JsonSerializable
data class Metrics(
    val Metrics: List<Metric>,
    val NextToken: NextToken? = null,
    val OwningAccounts: List<String>? = null,
)
