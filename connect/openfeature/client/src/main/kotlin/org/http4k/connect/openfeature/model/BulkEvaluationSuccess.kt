package org.http4k.connect.openfeature.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class BulkEvaluationSuccess(
    val flags: List<EvaluationResult>,
    val metadata: Map<String, Any?>? = null
)

@JsonSerializable
data class EvaluationResult(
    val key: FlagKey,
    val value: Any? = null,
    val reason: Reason? = null,
    val variant: String? = null,
    val errorCode: ErrorCode? = null,
    val errorDetails: String? = null,
    val metadata: Map<String, Any?>? = null
)
