package org.http4k.connect.openfeature.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class EvaluationFailure(
    val key: FlagKey,
    val errorCode: ErrorCode,
    val errorDetails: String? = null
)
