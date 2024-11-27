package org.http4k.connect.amazon.secretsmanager.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class Filter(val Key: String, val Values: List<String>)
