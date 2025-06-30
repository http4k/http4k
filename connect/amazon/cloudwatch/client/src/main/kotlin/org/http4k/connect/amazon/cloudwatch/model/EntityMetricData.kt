package org.http4k.connect.amazon.cloudwatch.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class EntityMetricData(
    val Entity: Entity? = null,
    val MetricData: List<MetricDatum>? = null,
)
