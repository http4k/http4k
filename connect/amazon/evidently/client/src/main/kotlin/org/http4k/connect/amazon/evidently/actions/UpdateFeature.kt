package org.http4k.connect.amazon.evidently.actions

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.evidently.EvidentlyAction
import org.http4k.connect.amazon.evidently.model.EntityId
import org.http4k.connect.amazon.evidently.model.EvaluationStrategy
import org.http4k.connect.amazon.evidently.model.FeatureName
import org.http4k.connect.amazon.evidently.model.FeatureResponse
import org.http4k.connect.amazon.evidently.model.ProjectName
import org.http4k.connect.amazon.evidently.model.VariationConfig
import org.http4k.connect.amazon.evidently.model.VariationName
import org.http4k.core.Method.PATCH
import org.http4k.core.Uri
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
data class UpdateFeature(
    val project: ProjectName,
    val feature: FeatureName,
    val addOrUpdateVariations: List<VariationConfig>? = null,
    val defaultVariation: VariationName? = null,
    val description: String? = null,
    val entityOverrides: Map<EntityId, VariationName>? = null,
    val evaluationStrategy: EvaluationStrategy? = null,
    val removeVariations: List<VariationName>? = null
) : EvidentlyAction<FeatureResponse>(FeatureResponse::class, method = PATCH) {

    override fun uri() = Uri.of("/projects/$project/features/$feature")

    override fun requestBody() = UpdateFeatureData(
        addOrUpdateVariations = addOrUpdateVariations,
        defaultVariation = defaultVariation,
        description = description,
        entityOverrides = entityOverrides,
        evaluationStrategy = evaluationStrategy,
        removeVariations = removeVariations
    )
}

@JsonSerializable
data class UpdateFeatureData(
    val addOrUpdateVariations: List<VariationConfig>?,
    val defaultVariation: VariationName?,
    val description: String?,
    val entityOverrides: Map<EntityId, VariationName>?,
    val evaluationStrategy: EvaluationStrategy?,
    val removeVariations: List<VariationName>?
)
