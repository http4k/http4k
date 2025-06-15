package org.http4k.connect.amazon.cloudwatch.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class DimensionFilter(
    val Name: String,
    val Value: String? = null,
)
