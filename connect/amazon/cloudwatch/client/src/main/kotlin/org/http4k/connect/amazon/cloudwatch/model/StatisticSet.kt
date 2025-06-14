package org.http4k.connect.amazon.cloudwatch.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class StatisticSet(
    val Maximum: Double,
    val Minimum: Double,
    val SampleCount: Double,
    val Sum: Double,
)
