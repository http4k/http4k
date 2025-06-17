package org.http4k.connect.amazon.cloudwatch.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class MetricStat(
    val Metric: Metric,
    val Stat: String,
    val Period: Int? = null,
    val Unit: MetricUnit? = null,
)
