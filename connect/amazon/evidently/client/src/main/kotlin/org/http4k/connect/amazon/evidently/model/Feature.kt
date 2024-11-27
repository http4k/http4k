package org.http4k.connect.amazon.evidently.model

import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.model.Timestamp
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class Feature(
    val arn: ARN,
    val createdTime: Timestamp,
    val evaluationStrategy: EvaluationStrategy,
    val lastUpdatedTime: Timestamp,
    val name: FeatureName,
    val status: FeatureStatus,
    val valueType: ValueType,
    val variations: List<VariationConfig>,
    val defaultVariation: VariationName,
    val description: String?,
    val entityOverrides: Map<String, VariationName>?,
    val evaluationRules: List<EvaluationRule>?,
    val project: ARN,
    val tags: Map<String, String>?
)

enum class FeatureStatus { AVAILABLE, UPDATING }

enum class ValueType { STRING, LONG, DOUBLE, BOOLEAN }

@JsonSerializable
data class EvaluationRule(
    val type: String,
    val name: String
)

@JsonSerializable
data class VariationConfig(
    val name: VariationName,
    val value: VariableValue
)

@JsonSerializable
data class VariableValue(
    val boolValue: Boolean?,
    val doubleValue: Double?,
    val longValue: Long?,
    val stringValue: String?
) {
    constructor(boolValue: Boolean) : this(
        boolValue = boolValue,
        doubleValue = null,
        longValue = null,
        stringValue = null
    )

    constructor(doubleValue: Double) : this(
        boolValue = null,
        doubleValue = doubleValue,
        longValue = null,
        stringValue = null
    )

    constructor(longValue: Long) : this(boolValue = null, doubleValue = null, longValue = longValue, stringValue = null)
    constructor(stringValue: String) : this(
        boolValue = null,
        doubleValue = null,
        longValue = null,
        stringValue = stringValue
    )
}
