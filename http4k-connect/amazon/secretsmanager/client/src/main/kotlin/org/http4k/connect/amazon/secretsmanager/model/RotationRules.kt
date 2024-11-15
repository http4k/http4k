package org.http4k.connect.amazon.secretsmanager.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class RotationRules(val AutomaticallyAfterDays: Int? = null)
