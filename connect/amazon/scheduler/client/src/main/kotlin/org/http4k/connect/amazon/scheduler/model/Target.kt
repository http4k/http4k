package org.http4k.connect.amazon.scheduler.model

import com.squareup.moshi.Json
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.core.model.Tag
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class Target(
    @Json(name = "Arn") val arn: ARN,
    @Json(name = "RoleArn") val roleArn: ARN,
    @Json(name = "Input") val input: String,
    @Json(name = "DeadLetterConfig") val deadLetterConfig: DeadLetterConfig? = null,
    @Json(name = "RetryPolicy") val retryPolicy: RetryPolicy? = null,
    @Json(name = "EcsParameters") val ecsParameters: EcsParameters? = null,
    @Json(name = "EventBridgeParameters") val eventBridgeParameters: EventBridgeParameters? = null,
    @Json(name = "SqsParameters") val sqsParameters: SqsParameters? = null
)

@JsonSerializable
data class DeadLetterConfig(
    @Json(name = "Arn") val arn: ARN
)

@JsonSerializable
data class RetryPolicy(
    @Json(name = "MaximumEventAgeInSeconds") val maximumEventAgeInSeconds: Int?,
    @Json(name = "MaximumRetryAttempts") val maximumRetryAttempts: Int?,
)

@JsonSerializable
data class EcsParameters(
    @Json(name = "TaskDefinitionArn") val taskDefinitionArn: ARN,
    @Json(name = "CapacityProviderStrategy") val capacityProviderStrategy: List<CapacityProviderStrategyItem>? = null,
    @Json(name = "EnableECSManagedTags") val enableECSManagedTags: Boolean? = null,
    @Json(name = "EnableExecuteCommand") val enableExecuteCommand: Boolean? = null,
    @Json(name = "Group") val group: String? = null,
    @Json(name = "LaunchType") val launchType: LaunchType? = null,
    @Json(name = "NetworkConfiguration") val networkConfiguration: NetworkConfiguration? = null,
    @Json(name = "PlacementConstraints") val placementConstraints: List<PlacementConstraint>? = null,
    @Json(name = "PlacementStrategy") val placementStrategy: List<PlacementStrategy>? = null,
    @Json(name = "PlatformVersion") val platformVersion: String? = null,
    @Json(name = "PropagateTags") val propagateTags: String? = null,
    @Json(name = "ReferenceId") val referenceId: String? = null,
    @Json(name = "TaskCount") val taskCount: Int? = null,
    @Json(name = "Tags") val tags: List<Tag>? = null
)

@JsonSerializable
data class CapacityProviderStrategyItem(
    val capacityProvider: String,
    val base: Int?,
    val weight: Int?
)

@JsonSerializable
data class NetworkConfiguration(
    val awsvpcConfiguration: AwsVpcConfiguration
)

@JsonSerializable
data class AwsVpcConfiguration(
    @Json(name = "Subnets") val subnets: List<String>,
    @Json(name = "AssignPublicIp") val assignPublicIp: AssignPublicIp?,
    @Json(name = "SecurityGroups") val securityGroups: List<String>?
)

@JsonSerializable
data class PlacementConstraint(
    val expression: String?,
    val type: String?
)

@JsonSerializable
data class PlacementStrategy(
    val field: String?,
    val type: String?
)

enum class LaunchType {
    EC2, FARGATE, EXTERNAL
}

enum class AssignPublicIp {
    ENABLED, DISABLED
}


@JsonSerializable
data class EventBridgeParameters(
    @Json(name = "DetailType") val detailType: String,
    @Json(name = "Source") val source: String,
)


@JsonSerializable
data class SqsParameters(
    @Json(name = "MessageGroupId") val messageGroupId: String?,
)
