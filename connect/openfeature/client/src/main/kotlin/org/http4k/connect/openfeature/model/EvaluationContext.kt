package org.http4k.connect.openfeature.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class EvaluationContext(val context: Map<String, Any?>) {
    companion object {
        operator fun invoke(targetingKey: TargetingKey, vararg attributes: Pair<String, Any?>): EvaluationContext =
            EvaluationContext(mapOf("targetingKey" to targetingKey.value) + attributes.toMap())

        operator fun invoke(vararg attributes: Pair<String, Any?>): EvaluationContext =
            EvaluationContext(attributes.toMap())
    }
}
