package org.http4k.connect.amazon.evidently

import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.core.model.AwsAccount
import org.http4k.connect.amazon.core.model.Region
import org.http4k.connect.amazon.evidently.actions.Project
import org.http4k.connect.amazon.evidently.model.ProjectName
import org.http4k.connect.model.Timestamp
import org.http4k.connect.storage.Storage
import java.time.Instant

data class StoredProject(
    val accountId: AwsAccount,
    val region: Region,
    val name: ProjectName,
    val description: String?,
    val tags: Map<String, String>?,
    val created: Instant,
    val updated: Instant = created
)

val StoredProject.arn get() = ARN.of(Evidently.awsService, region, accountId, "project", name)

fun StoredProject.toProject(features: Storage<StoredFeature>) = Project(
    name = name,
    description = description,
    tags = tags,
    featureCount = features.keySet().count { it == name.value },
    arn = arn,
    createdTime = Timestamp.of(created),
    activeExperimentCount = null,
    activeLaunchCount = null,
    lastUpdatedTime = Timestamp.of(updated),
    experimentCount = null,
    launchCount = null,
    status = "AVAILABLE"
)
