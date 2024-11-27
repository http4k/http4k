package org.http4k.connect.amazon.secretsmanager.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.secretsmanager.SecretsManagerAction
import org.http4k.connect.amazon.secretsmanager.model.SecretId
import org.http4k.connect.amazon.secretsmanager.model.VersionId
import org.http4k.connect.amazon.secretsmanager.model.VersionStage
import org.http4k.connect.model.Base64Blob
import org.http4k.connect.model.Timestamp
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
@JsonSerializable
data class GetSecretValue(
    val SecretId: SecretId,
    val VersionId: VersionId? = null,
    val VersionStage: VersionStage? = null
) : SecretsManagerAction<SecretValue>(SecretValue::class)

@JsonSerializable
data class SecretValue(
    val ARN: ARN,
    val CreatedDate: Timestamp,
    val Name: String,
    val SecretBinary: Base64Blob? = null,
    val SecretString: String? = null,
    val VersionId: VersionId,
    val VersionStages: List<VersionStage>
)
