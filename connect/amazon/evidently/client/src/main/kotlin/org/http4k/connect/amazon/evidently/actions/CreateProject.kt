package org.http4k.connect.amazon.evidently.actions

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.evidently.EvidentlyAction
import org.http4k.connect.amazon.evidently.model.ProjectName
import org.http4k.connect.model.Timestamp
import org.http4k.core.Uri
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
data class CreateProject(
    val name: ProjectName,
    val description: String?,
    val tags: Map<String, String>?
) : EvidentlyAction<CreateProjectResponse>(CreateProjectResponse::class) {
    override fun uri() = Uri.of("/projects")

    override fun requestBody() = CreateProjectData(
        name = name,
        description = description,
        tags = tags
    )
}

@JsonSerializable
data class CreateProjectData(
    val name: ProjectName,
    val description: String?,
    val tags: Map<String, String>?,
    // appConfigResource,
    // dataDelivery
)


@JsonSerializable
data class Project(
    val activeExperimentCount: Int?,
    val activeLaunchCount: Int?,
    // appConfigResource
    val arn: ARN,
    val createdTime: Timestamp,
    // dataDelivery
    val description: String?,
    val experimentCount: Int?,
    val featureCount: Int?,
    val lastUpdatedTime: Timestamp,
    val launchCount: Int?,
    val name: ProjectName,
    val status: String,
    val tags: Map<String, String>?
)

@JsonSerializable
data class CreateProjectResponse(
    val project: Project
)
