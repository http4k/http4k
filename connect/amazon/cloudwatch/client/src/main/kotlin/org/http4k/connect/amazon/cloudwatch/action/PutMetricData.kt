package org.http4k.connect.amazon.cloudwatch.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.cloudwatch.CloudWatchAction
import org.http4k.connect.amazon.cloudwatch.model.EntityMetricData
import org.http4k.connect.amazon.cloudwatch.model.MetricDatum
import org.http4k.connect.amazon.cloudwatch.model.Namespace
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
@JsonSerializable
data class PutMetricData(
    val Namespace: Namespace,
    val EntityMetricData: List<EntityMetricData>? = null,
    val MetricData: List<MetricDatum>? = null,
    val StrictEntityValidation: Boolean? = null,
) : CloudWatchAction<Unit>(Unit::class)
