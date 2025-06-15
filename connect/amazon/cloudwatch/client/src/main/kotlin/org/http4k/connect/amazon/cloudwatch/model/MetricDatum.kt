package org.http4k.connect.amazon.cloudwatch.model

import se.ansman.kotshi.JsonSerializable
import java.time.Instant

@JsonSerializable
data class MetricDatum(
    val MetricName: MetricName,
    val Counts: List<Double>? = null,
    val Dimensions: List<Dimension>? = null,
    val StatisticValues: StatisticSet? = null,
    val StorageResolution: Int? = null,
    val Timestamp: Instant? = null,
    val Unit: MetricUnit? = null,
    val Value: Double? = null,
    val Values: List<Double>? = null,
)
