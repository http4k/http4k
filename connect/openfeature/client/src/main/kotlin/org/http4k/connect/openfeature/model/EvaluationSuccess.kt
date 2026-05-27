package org.http4k.connect.openfeature.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class EvaluationSuccess(
    val key: FlagKey,
    val value: Any?,
    val reason: Reason? = null,
    val variant: String? = null,
    val metadata: Map<String, Any?>? = null
)
