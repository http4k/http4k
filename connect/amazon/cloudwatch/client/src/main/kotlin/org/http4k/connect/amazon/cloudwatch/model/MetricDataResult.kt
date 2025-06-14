package org.http4k.connect.amazon.cloudwatch.model

import se.ansman.kotshi.JsonSerializable
import java.time.Instant

@JsonSerializable
data class MetricDataResult(
    val Id: String? = null,
    val Label: String? = null,
    val Messages: List<MessageData>? = null,
    val StatusCode: StatusCode? = null,
    val Timestamps: List<Instant>? = null,
    val Values: List<Double>? = null,
)
