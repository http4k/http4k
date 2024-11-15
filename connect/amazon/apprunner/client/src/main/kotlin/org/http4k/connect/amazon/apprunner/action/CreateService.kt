package org.http4k.connect.amazon.apprunner.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.apprunner.AppRunnerAction
import org.http4k.connect.amazon.apprunner.model.AppRunnerService
import org.http4k.connect.amazon.apprunner.model.ServiceId
import org.http4k.connect.amazon.apprunner.model.ServiceName
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.core.model.KMSKeyId
import org.http4k.connect.amazon.core.model.Tag
import org.http4k.connect.kClass
import org.http4k.connect.model.Timestamp
import org.http4k.core.Uri
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
@JsonSerializable
data class CreateService(
    val ServiceName: ServiceName,
    val SourceConfiguration: SourceConfiguration,
    val AutoScalingConfigurationArn: ARN? = null,
    val EncryptionConfiguration: EncryptionConfiguration? = null,
    val HealthCheckConfiguration: HealthCheckConfiguration? = null,
    val InstanceConfiguration: InstanceConfiguration? = null,
    val NetworkConfiguration: NetworkConfiguration? = null,
    val ObservabilityConfiguration: ObservabilityConfiguration? = null,
    val Tags: List<Tag>? = null
) : AppRunnerAction<AppRunnerService>(kClass())

@JsonSerializable
data class SourceConfiguration(
    val AuthenticationConfiguration: AuthenticationConfiguration? = null,
    val AutoDeploymentsEnabled: Boolean? = null,
    val CodeRepository: CodeRepository? = null,
    val ImageRepository: ImageRepository? = null
)

@JsonSerializable
data class EncryptionConfiguration(
    val KmsKey: KMSKeyId?
)

@JsonSerializable
data class HealthCheckConfiguration(
    val HealthyThreshold: Int?,
    val Interval: Int?,
    val Path: String?,
    val Protocol: String?,
    val Timeout: Int?,
    val UnhealthyThreshold: Int?
)

@JsonSerializable
data class InstanceConfiguration(
    val Cpu: String?,
    val InstanceRoleArn: ARN?,
    val Memory: String?
)

@JsonSerializable
data class EgressConfiguration(
    val EgressType: String?,
    val VpcConnectorArn: ARN?
)

@JsonSerializable
data class IngressConfiguration(
    val IsPubliclyAccessible: Boolean?
)

@JsonSerializable
data class NetworkConfiguration(
    val EgressConfiguration: EgressConfiguration?,
    val IngressConfiguration: IngressConfiguration?,
    val IpAddressType: String?
)

@JsonSerializable
data class ObservabilityConfiguration(
    val ObservabilityEnabled: Boolean,
    val ObservabilityConfigurationArn: String?
)

@JsonSerializable
data class AuthenticationConfiguration(
    val AccessRoleArn: ARN?,
    val ConnectionArn: String?
)

@JsonSerializable
data class CodeConfigurationValues(
    val BuildCommand: String?,
    val Port: String?,
    val Runtime: String?,
    val RuntimeEnvironmentSecrets: Map<String, String>,
    val RuntimeEnvironmentVariables: Map<String, String>,
    val StartCommand: String?
)

@JsonSerializable
data class CodeConfiguration(
    val CodeConfigurationValues: CodeConfigurationValues?,
    val ConfigurationSource: String?
)

@JsonSerializable
data class SourceCodeVersion(
    val Type: String?,
    val Value: String?
)

@JsonSerializable
data class CodeRepository(
    val CodeConfiguration: CodeConfiguration?,
    val RepositoryUrl: Uri?,
    val SourceCodeVersion: SourceCodeVersion?,
    val SourceDirectory: String?
)

@JsonSerializable
data class ImageConfiguration(
    val Port: String?,
    val RuntimeEnvironmentSecrets: Map<String, String>,
    val RuntimeEnvironmentVariables: Map<String, String>,
    val StartCommand: String?
)

@JsonSerializable
data class ImageRepository(
    val ImageConfiguration: ImageConfiguration?,
    val ImageIdentifier: String?,
    val ImageRepositoryType: String?
)

@JsonSerializable
data class AutoScalingConfigurationSummary(
    val AutoScalingConfigurationArn: ARN?,
    val AutoScalingConfigurationName: String?,
    val AutoScalingConfigurationRevision: Int?,
    val CreatedAt: Timestamp?,
    val HasAssociatedService: Boolean?,
    val IsDefault: Boolean?,
    val Status: String?
)

@JsonSerializable
data class Service(
    val ServiceArn: ARN,
    val ServiceId: ServiceId,
    val ServiceName: ServiceName,
    val Status: String,
    val CreatedAt: Timestamp,
    val UpdatedAt: Timestamp,
    val ServiceUrl: Uri? = null,
    val DeletedAt: Timestamp? = null,
    val AutoScalingConfigurationSummary: AutoScalingConfigurationSummary? = null,
    val EncryptionConfiguration: EncryptionConfiguration? = null,
    val HealthCheckConfiguration: HealthCheckConfiguration? = null,
    val InstanceConfiguration: InstanceConfiguration? = null,
    val NetworkConfiguration: NetworkConfiguration? = null,
    val ObservabilityConfiguration: ObservabilityConfiguration? = null,
    val SourceConfiguration: SourceConfiguration? = null,
)

