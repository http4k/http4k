package org.http4k.connect.amazon.evidently.actions

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.evidently.EvidentlyAction
import org.http4k.connect.amazon.evidently.model.EntityId
import org.http4k.connect.amazon.evidently.model.EvaluationContext
import org.http4k.connect.amazon.evidently.model.FeatureName
import org.http4k.connect.amazon.evidently.model.ProjectName
import org.http4k.connect.amazon.evidently.model.VariationName
import org.http4k.core.Uri
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
data class BatchEvaluateFeature(
    val project: ProjectName,
    val requests: List<BatchEvaluationRequest>
) : EvidentlyAction<BatchEvaluationResultWrapper>(BatchEvaluationResultWrapper::class, dataPlane = true) {
    override fun uri() = Uri.of("/projects/$project/evaluations")

    override fun requestBody() = BatchEvaluateFeatureRequestWrapper(requests)
}

@JsonSerializable
data class BatchEvaluateFeatureRequestWrapper(
    val requests: List<BatchEvaluationRequest>
)

@JsonSerializable
data class BatchEvaluationRequest(
    val entityId: EntityId,
    val feature: FeatureName,
    val evaluationContext: EvaluationContext? = null,
)

@JsonSerializable
data class BatchEvaluationResultWrapper(
    val results: List<BatchEvaluateFeatureResult>
)

@JsonSerializable
data class BatchEvaluateFeatureResult(
    val entityId: EntityId,
    val feature: ARN,
    val project: ARN?,
    val variation: VariationName? = null,
    val value: VariableValue? = null,
    val details: String? = null,
    val reason: String? = null
)
