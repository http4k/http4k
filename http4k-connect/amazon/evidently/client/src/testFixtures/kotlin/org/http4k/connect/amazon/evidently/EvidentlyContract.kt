package org.http4k.connect.amazon.evidently

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.failureOrNull
import org.http4k.connect.RemoteFailure
import org.http4k.connect.amazon.AwsContract
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.evidently.actions.BatchEvaluateFeatureResult
import org.http4k.connect.amazon.evidently.actions.BatchEvaluationRequest
import org.http4k.connect.amazon.evidently.actions.BatchEvaluationResultWrapper
import org.http4k.connect.amazon.evidently.actions.EvaluatedFeature
import org.http4k.connect.amazon.evidently.actions.VariableValue
import org.http4k.connect.amazon.evidently.model.EntityId
import org.http4k.connect.amazon.evidently.model.EvaluationStrategy
import org.http4k.connect.amazon.evidently.model.FeatureName
import org.http4k.connect.amazon.evidently.model.ProjectName
import org.http4k.connect.amazon.evidently.model.VariationConfig
import org.http4k.connect.amazon.evidently.model.VariationName
import org.http4k.connect.successValue
import org.http4k.core.HttpHandler
import org.http4k.core.Method.DELETE
import org.http4k.core.Method.PATCH
import org.http4k.core.Method.POST
import org.http4k.core.Status
import org.http4k.core.Status.Companion.CONFLICT
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Uri
import org.http4k.lens.StringBiDiMappings.uuid
import org.junit.jupiter.api.Test
import java.util.UUID

interface EvidentlyContract : AwsContract {
    private val evidently
        get() =
        Evidently.Http(aws.region, { aws.credentials }, http)

    private val projectName get() = ProjectName.of(uuid(0).toString())
    private val missingProjectName get() = ProjectName.of(uuid(11).toString())
    private val featureName get() = FeatureName.of(uuid(1).toString())
    private val missingFeatureName get() = FeatureName.of(uuid(2).toString())
    private val entity1 get() = EntityId.of(uuid(1).toString())
    private val entity2 get() = EntityId.of(uuid(2).toString())

    @Test
    fun `project lifecycle`() = try {
        evidently.createProject(
            name = projectName,
            description = "stuff",
            tags = mapOf("foo" to "bar")
        ).successValue()

        val feature = evidently.createFeature(
            project = projectName,
            name = featureName,
            variations = mapOf(
                VariationName.of("foo") to VariableValue("123"),
                VariationName.of("bar") to VariableValue("456")
            ),
            description = "feature stuff",
            defaultVariation = VariationName.of("foo"),
            tags = mapOf("toll" to "troll"),
            entityOverrides = mapOf(
                entity1 to VariationName.of("bar")
            )
        ).successValue().feature

        val missingProjectArn = ARN.of(feature.project.value.replace(projectName.value, missingProjectName.value))
        val missingFeatureArn = ARN.of(feature.arn.value.replace(featureName.value, missingFeatureName.value))

        // evaluate by feature override
        val evaluation1 = evidently.evaluateFeature(
            project = projectName,
            feature = featureName,
            entityId = entity1
        ).successValue()
        assertThat(
            evaluation1, equalTo(
                EvaluatedFeature(
                    details = "{}",
                    reason = "ENTITY_OVERRIDES_MATCH",
                    value = VariableValue("456"),
                    variation = VariationName.of("bar")
                )
            )
        )

        // evaluate by default variation
        val evaluation2 = evidently.evaluateFeature(
            project = projectName,
            feature = featureName,
            entityId = entity2
        ).successValue()
        assertThat(
            evaluation2, equalTo(
                EvaluatedFeature(
                    details = "{}",
                    reason = "DEFAULT",
                    value = VariableValue("123"),
                    variation = VariationName.of("foo")
                )
            )
        )

        // evaluate missing feature
        val evaluation3 = evidently.evaluateFeature(
            project = projectName,
            feature = missingFeatureName,
            entityId = entity1
        ).failureOrNull()
        assertThat(
            evaluation3, equalTo(
                RemoteFailure(
                    method = POST,
                    uri = Uri.of("/projects/$projectName/evaluations/$missingFeatureName"),
                    status = NOT_FOUND,
                    message = "{\"message\":\"Feature does not exist with arn '$missingFeatureArn'\",\"resourceId\":null,\"resourceType\":null}"
                )
            )
        )

        // evaluate feature for missing project
        val evaluation4 = evidently.evaluateFeature(
            project = missingProjectName,
            feature = missingFeatureName,
            entityId = entity1
        ).failureOrNull()
        assertThat(
            evaluation4, equalTo(
                RemoteFailure(
                    method = POST,
                    uri = Uri.of("/projects/$missingProjectName/evaluations/$missingFeatureName"),
                    status = NOT_FOUND,
                    message = "{\"message\":\"Project does not exist with arn '$missingProjectArn'\",\"resourceId\":null,\"resourceType\":null}"
                )
            )
        )

        // batch evaluate
        val batchEvaluation = evidently.batchEvaluateFeature(
            project = projectName,
            requests = listOf(
                BatchEvaluationRequest(entity1, featureName),
                BatchEvaluationRequest(entity2, missingFeatureName)
            )
        ).successValue()
        assertThat(
            batchEvaluation, equalTo(
                BatchEvaluationResultWrapper(
                    listOf(
                        BatchEvaluateFeatureResult(
                            entity1,
                            feature.arn,
                            feature.project,
                            VariationName.of("bar"),
                            VariableValue("456"),
                            details = "{}",
                            reason = "ENTITY_OVERRIDES_MATCH"
                        ),
                        BatchEvaluateFeatureResult(
                            entity2,
                            missingFeatureArn,
                            feature.project,
                            reason = "Feature does not exist with arn '$missingFeatureArn'"
                        )
                    )
                )
            )
        )

        // batch evaluate missing project
        val batchResult = evidently.batchEvaluateFeature(
            project = missingProjectName,
            requests = listOf(
                BatchEvaluationRequest(entity1, featureName)
            )
        ).failureOrNull()
        assertThat(
            batchResult, equalTo(
                RemoteFailure(
                    method = POST,
                    uri = Uri.of("/projects/$missingProjectName/evaluations"),
                    status = NOT_FOUND,
                    message = "{\"message\":\"Project does not exist with arn '$missingProjectArn'\",\"resourceId\":null,\"resourceType\":null}"
                )
            )
        )

        // update missing feature
        assertThat(
            evidently.updateFeature(projectName, missingFeatureName).failureOrNull(), equalTo(
                RemoteFailure(
                    method = PATCH,
                    uri = Uri.of("/projects/$projectName/features/$missingFeatureName"),
                    status = NOT_FOUND,
                    message = "{\"message\":\"Feature with arn '$missingFeatureArn' does not exist.\",\"resourceId\":\"$missingFeatureArn\",\"resourceType\":\"feature\"}"
                )
            )
        )

        // update feature
        evidently.updateFeature(
            project = projectName,
            feature = featureName,
            evaluationStrategy = EvaluationStrategy.ALL_RULES,
            defaultVariation = VariationName.of("bar"),
            addOrUpdateVariations = listOf(
                VariationConfig(VariationName.of("baz"), VariableValue("789"))
            ),
            entityOverrides = mapOf(
                entity2 to VariationName.of("baz")
            ),
            removeVariations = listOf(VariationName.of("foo")),
            description = "updated"
        ).successValue().also {
            assertThat(it.feature.description, equalTo("updated"))
            assertThat(it.feature.evaluationStrategy, equalTo(EvaluationStrategy.ALL_RULES))
            assertThat(it.feature.defaultVariation, equalTo(VariationName.of("bar")))
            assertThat(
                it.feature.variations, equalTo(
                    listOf(
                        VariationConfig(VariationName.of("bar"), VariableValue("456")),
                        VariationConfig(VariationName.of("baz"), VariableValue("789"))
                    )
                )
            )
            assertThat(
                it.feature.entityOverrides, equalTo(
                    mapOf(
                        entity2.value to VariationName.of("baz")
                    )
                )
            )
        }

        // cannot delete project with features
        assertThat(
            evidently.deleteProject(projectName).failureOrNull(), equalTo(
                RemoteFailure(
                    status = CONFLICT,
                    uri = Uri.of("/projects/$projectName"),
                    method = DELETE,
                    message = "{\"message\":\"Project has sub-resources\",\"resourceId\":\"${feature.project}\",\"resourceType\":\"project\"}",
                )
            )
        )

        // delete feature from missing project
        assertThat(
            evidently.deleteFeature(missingProjectName, featureName), equalTo(
                Failure(
                    RemoteFailure(
                        status = Status(404, ""),
                        uri = Uri.of("/projects/$missingProjectName/features/$featureName"),
                        method = DELETE,
                        message = "{\"message\":\"Project with arn '$missingProjectArn/feature/$featureName' does not exist.\",\"resourceId\":\"$missingProjectArn/feature/$featureName\",\"resourceType\":\"project\"}"
                    )
                )
            )
        )

        evidently.deleteFeature(projectName, featureName).successValue()

        evidently.deleteProject(projectName).successValue()
    } finally {
        evidently.deleteFeature(projectName, featureName)
        evidently.deleteProject(projectName)
    }
}
