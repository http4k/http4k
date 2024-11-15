package org.http4k.connect.amazon.evidently

import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.evidently.actions.EvaluatedFeature
import org.http4k.connect.amazon.evidently.model.EntityId
import org.http4k.connect.amazon.evidently.model.EvaluationStrategy
import org.http4k.connect.amazon.evidently.model.Feature
import org.http4k.connect.amazon.evidently.model.FeatureName
import org.http4k.connect.amazon.evidently.model.FeatureStatus
import org.http4k.connect.amazon.evidently.model.ValueType
import org.http4k.connect.amazon.evidently.model.VariableValue
import org.http4k.connect.amazon.evidently.model.VariationConfig
import org.http4k.connect.amazon.evidently.model.VariationName
import org.http4k.connect.model.Timestamp
import java.time.Instant

data class StoredFeature(
    val name: FeatureName,
    val projectArn: ARN,
    val default: VariationName,
    val variations: Map<VariationName, VariationConfig>,
    val overrides: Map<EntityId, VariationName>,
    val created: Instant,
    val updated: Instant = created,
    val description: String?,
    val evaluationStrategy: EvaluationStrategy,
    val tags: Map<String, String>?
)

val VariableValue.type
    get() = when {
        stringValue != null -> ValueType.STRING
        boolValue != null -> ValueType.BOOLEAN
        longValue != null -> ValueType.LONG
        doubleValue != null -> ValueType.DOUBLE
        else -> error("Illegal state")
    }

val StoredFeature.arn get() = ARN.of("$projectArn/feature/$name")

operator fun StoredFeature.get(entityId: EntityId): EvaluatedFeature {
    val variation = variations.getValue(overrides[entityId] ?: default)

    return EvaluatedFeature(
        details = "{}",    // {\"launch\":\"launch\",\"group\":\"V1\"}
        reason = when {
            entityId in overrides -> "ENTITY_OVERRIDES_MATCH"
            // TODO LAUNCH_RULE_MATCH
            else -> "DEFAULT"
        },
        variation = variation.name,
        value = variation.value
    )
}

fun StoredFeature.toFeature() = Feature(
    name = name,
    defaultVariation = default,
    entityOverrides = overrides.mapKeys { it.key.value },
    arn = arn,
    createdTime = Timestamp.of(created),
    lastUpdatedTime = Timestamp.of(updated),
    description = description,
    evaluationRules = null,
    evaluationStrategy = evaluationStrategy,
    project = projectArn,
    status = FeatureStatus.AVAILABLE,
    tags = tags,
    valueType = variations.getValue(default).value.type,
    variations = variations.values.toList()
)
