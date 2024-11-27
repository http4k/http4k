package org.http4k.connect.amazon.evidently.actions

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.evidently.EvidentlyAction
import org.http4k.connect.amazon.evidently.model.EntityId
import org.http4k.connect.amazon.evidently.model.EvaluationStrategy
import org.http4k.connect.amazon.evidently.model.FeatureName
import org.http4k.connect.amazon.evidently.model.FeatureResponse
import org.http4k.connect.amazon.evidently.model.ProjectName
import org.http4k.connect.amazon.evidently.model.VariableValue
import org.http4k.connect.amazon.evidently.model.VariationConfig
import org.http4k.connect.amazon.evidently.model.VariationName
import org.http4k.core.Uri
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
data class CreateFeature(
    val project: ProjectName,
    val defaultVariation: VariationName,
    val description: String?,
    val entityOverrides: Map<EntityId, VariationName>?,
    val evaluationStrategy: EvaluationStrategy?,
    val name: FeatureName,
    val tags: Map<String, String>?,
    val variations: Map<VariationName, VariableValue>
) : EvidentlyAction<FeatureResponse>(FeatureResponse::class) {

    override fun uri() = Uri.of("/projects/$project/features")

    override fun requestBody() = CreateFeatureData(
        defaultVariation = defaultVariation,
        description = description,
        entityOverrides = entityOverrides?.mapKeys { it.key.value },
        evaluationStrategy = evaluationStrategy,
        name = name,
        tags = tags,
        variations = variations.map { VariationConfig(it.key, it.value) }
    )
}

@JsonSerializable
data class CreateFeatureData(
    val defaultVariation: VariationName,
    val description: String?,
    val entityOverrides: Map<String, VariationName>?,
    val evaluationStrategy: EvaluationStrategy?,
    val name: FeatureName,
    val tags: Map<String, String>?,
    val variations: List<VariationConfig>
)


