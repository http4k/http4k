package org.http4k.connect.amazon.cloudwatch.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class Metric(
    val MetricName: MetricName,
    val Namespace: Namespace? = null,
    val Dimensions: List<Dimension>? = null,
)
