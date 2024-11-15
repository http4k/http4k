package org.http4k.connect.amazon.core.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class Tag(val Key: String, val Value: String)
