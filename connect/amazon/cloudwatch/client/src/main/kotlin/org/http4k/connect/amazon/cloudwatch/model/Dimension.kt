package org.http4k.connect.amazon.cloudwatch.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class Dimension(
    val Name: String,
    val Value: String,
)
