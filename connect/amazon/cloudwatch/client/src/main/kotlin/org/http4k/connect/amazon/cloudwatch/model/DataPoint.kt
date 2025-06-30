package org.http4k.connect.amazon.cloudwatch.model

import se.ansman.kotshi.JsonSerializable
import java.time.Instant

@JsonSerializable
data class DataPoint(
    val Average: Double? = null,
    val ExtendedStatistics: Map<String, Double>? = null,
    val Maximum: Double? = null,
    val Minimum: Double? = null,
    val SampleCount: Double? = null,
    val Sum: Double? = null,
    val Timestamp: Instant? = null,
    val Unit: MetricUnit? = null,
)
