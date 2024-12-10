package org.http4k.connect.amazon.evidently

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import dev.forkhandles.result4k.asResultOr
import dev.forkhandles.result4k.flatMap
import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.peek
import org.http4k.connect.amazon.AwsRestJsonFake
import org.http4k.connect.amazon.RestfulError
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.evidently.actions.BatchEvaluateFeatureRequestWrapper
import org.http4k.connect.amazon.evidently.actions.BatchEvaluateFeatureResult
import org.http4k.connect.amazon.evidently.actions.BatchEvaluationResultWrapper
import org.http4k.connect.amazon.evidently.actions.CreateFeatureData
import org.http4k.connect.amazon.evidently.actions.CreateProjectData
import org.http4k.connect.amazon.evidently.actions.CreateProjectResponse
import org.http4k.connect.amazon.evidently.actions.EvaluateFeatureRequest
import org.http4k.connect.amazon.evidently.actions.UpdateFeatureData
import org.http4k.connect.amazon.evidently.model.EntityId
import org.http4k.connect.amazon.evidently.model.EvaluationStrategy
import org.http4k.connect.amazon.evidently.model.FeatureName
import org.http4k.connect.amazon.evidently.model.FeatureResponse
import org.http4k.connect.amazon.evidently.model.ProjectName
import org.http4k.connect.storage.Storage
import org.http4k.core.Method.DELETE
import org.http4k.core.Method.PATCH
import org.http4k.core.Method.POST
import org.http4k.core.Status
import org.http4k.core.Status.Companion.CONFLICT
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.lens.Path
import org.http4k.lens.value
import org.http4k.routing.bind
import java.time.Clock

private fun AwsRestJsonFake.projectNotFound(name: ProjectName) =
    RestfulError(NOT_FOUND, "Project does not exist with arn '${arn("project", name)}'", null, null)

private fun AwsRestJsonFake.projectResourceNotFound(resourceType: String, resourcePath: String): RestfulError {
    val resourceArn = ARN.of(awsService, region, accountId, resourcePath)
    val message = "${resourceType.replaceFirstChar(Char::uppercaseChar)} with arn '$resourceArn' does not exist."
    return RestfulError(Status(404, ""), message, resourceArn, resourceType)
}

private fun featureNotFound(project: StoredProject, featureName: FeatureName) =
    RestfulError(NOT_FOUND, "Feature does not exist with arn '${project.arn}/feature/$featureName'", null, null)

private val projectLens = Path.value(ProjectName).of("project")
private val featureLens = Path.value(FeatureName).of("feature")

private operator fun Storage<StoredProject>.get(projectName: ProjectName) = get(projectName.value)
private operator fun Storage<StoredFeature>.get(featureName: FeatureName) = get(featureName.value)


fun AwsRestJsonFake.evaluateFeature(
    projects: Storage<StoredProject>,
    features: Storage<StoredFeature>
) = "/projects/$projectLens/evaluations/$featureLens" bind POST to route<EvaluateFeatureRequest> { data ->
    val projectName = projectLens(this)
    val featureName = featureLens(this)

    projects[projectName]
        .asResultOr { projectNotFound(projectName) }
        .flatMap { features["$projectName-$featureName"].asResultOr { featureNotFound(it, featureName) } }
        .map { it[data.entityId] }
}

fun AwsRestJsonFake.batchEvaluateFeature(
    projects: Storage<StoredProject>,
    features: Storage<StoredFeature>
) = "/projects/$projectLens/evaluations" bind POST to route<BatchEvaluateFeatureRequestWrapper> { data ->
    val projectName = projectLens(this)

    val project = projects[projectName] ?: return@route Failure(projectNotFound(projectName))

    val results = data.requests.map { request ->
        val feature = features["$projectName-${request.feature}"]
        val result = feature?.get(request.entityId)
        val featureArn = ARN.of("${project.arn}/feature/${request.feature}")
        BatchEvaluateFeatureResult(
            entityId = request.entityId,
            project = project.arn,
            feature = featureArn,
            variation = result?.variation,
            value = result?.value,
            details = result?.details,
            reason = result?.reason ?: "Feature does not exist with arn '$featureArn'"
        )
    }
    Success(BatchEvaluationResultWrapper(results))
}

fun AwsRestJsonFake.createProject(
    clock: Clock,
    projects: Storage<StoredProject>,
    features: Storage<StoredFeature>
) = "/projects" bind POST to route<CreateProjectData> { data ->
    val project = StoredProject(
        accountId = accountId,
        region = region,
        name = data.name,
        description = data.description,
        tags = data.tags,
        created = clock.instant()
    )

    projects[project.name.value] = project

    Success(CreateProjectResponse(project.toProject(features)))
}

fun AwsRestJsonFake.createFeature(
    clock: Clock,
    projects: Storage<StoredProject>,
    features: Storage<StoredFeature>,
) = "/projects/$projectLens/features" bind POST to route<CreateFeatureData> { data ->
    val projectName = projectLens(this)
    projects[projectName]
        .asResultOr { projectNotFound(projectName) }
        .map { project ->
            StoredFeature(
                name = data.name,
                default = data.defaultVariation,
                variations = data.variations.associateBy { it.name },
                overrides = data.entityOverrides.orEmpty().mapKeys { EntityId.of(it.key) },
                created = clock.instant(),
                description = data.description,
                evaluationStrategy = data.evaluationStrategy ?: EvaluationStrategy.ALL_RULES, // TODO fixme
                tags = data.tags,
                projectArn = project.arn
            )
        }.peek { features["$projectName-${it.name}"] = it }
        .map { FeatureResponse(it.toFeature()) }
}

fun AwsRestJsonFake.updateFeature(
    clock: Clock,
    projects: Storage<StoredProject>,
    features: Storage<StoredFeature>
) = "/projects/$projectLens/features/$featureLens" bind PATCH to route<UpdateFeatureData> { data ->
    val projectName = projectLens(this)
    val featureName = featureLens(this)
    val key = "$projectName-$featureName"

    projects[projectName]
        .asResultOr { projectResourceNotFound("project", "project:$projectName/feature/$featureName") }
        .flatMap {
            features[key].asResultOr {
                projectResourceNotFound(
                    "feature",
                    "project:$projectName/feature/$featureName"
                )
            }
        }
        .map { feature ->
            feature.copy(
                updated = clock.instant(),
                description = data.description ?: feature.description,
                evaluationStrategy = data.evaluationStrategy ?: feature.evaluationStrategy,
                overrides = data.entityOverrides ?: feature.overrides,
                default = data.defaultVariation ?: feature.default,
                variations = feature.variations
                    + data.addOrUpdateVariations.orEmpty().associateBy { it.name }
                    - data.removeVariations.orEmpty().toSet()
            )
        }
        .peek { features[key] = it }
        .map { FeatureResponse(it.toFeature()) }
}

fun AwsRestJsonFake.deleteFeature(
    projects: Storage<StoredProject>,
    features: Storage<StoredFeature>
) = "/projects/$projectLens/features/$featureLens" bind DELETE to route<Unit> {
    val projectName = projectLens(this)
    val featureName = featureLens(this)
    val key = "$projectName-$featureName"

    projects[projectName]
        .asResultOr { projectResourceNotFound("project", "project:$projectName/feature/$featureName") }
        .flatMap {
            features[key].asResultOr {
                projectResourceNotFound(
                    "feature",
                    "project:$projectName/feature/$featureName"
                )
            }
        }
        .peek { features.remove(key) }
        .map { }
}

fun AwsRestJsonFake.deleteProject(
    projects: Storage<StoredProject>,
    features: Storage<StoredFeature>
) = "/projects/$projectLens" bind DELETE to route<Unit> {
    val projectName = projectLens(this)

    projects[projectName]
        .asResultOr { projectNotFound(projectName) }
        .flatMap { project ->
            if (features.keySet(project.name.value).isEmpty()) {
                Success(project)
            } else {
                Failure(
                    RestfulError(
                        CONFLICT,
                        "Project has sub-resources",
                        arn("project", projectName),
                        resourceType = "project"
                    )
                )
            }
        }
        .peek { project -> projects.remove(project.name.value) }
        .map { }
}
