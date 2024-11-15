package org.http4k.connect.amazon.secretsmanager.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.secretsmanager.SecretsManagerAction
import org.http4k.connect.amazon.secretsmanager.model.SecretId
import org.http4k.connect.amazon.secretsmanager.model.VersionId
import org.http4k.connect.amazon.secretsmanager.model.VersionStage
import org.http4k.connect.model.Base64Blob
import se.ansman.kotshi.JsonSerializable
import java.util.UUID

@Http4kConnectAction
@JsonSerializable
@ConsistentCopyVisibility
data class PutSecretValue internal constructor(
    val SecretId: SecretId,
    val ClientRequestToken: UUID,
    val SecretString: String? = null,
    val SecretBinary: Base64Blob? = null,
    val VersionStages: List<VersionStage>? = null
) : SecretsManagerAction<UpdatedSecretValue>(UpdatedSecretValue::class) {
    constructor(
        SecretId: SecretId,
        ClientRequestToken: UUID,
        SecretString: String,
        VersionStages: List<VersionStage>? = null
    ) : this(SecretId, ClientRequestToken, SecretString, null, VersionStages)

    constructor(
        SecretId: SecretId,
        ClientRequestToken: UUID,
        SecretBinary: Base64Blob,
        VersionStages: List<VersionStage>? = null
    ) : this(SecretId, ClientRequestToken, null, SecretBinary, VersionStages)
}

@JsonSerializable
data class UpdatedSecretValue(
    val ARN: ARN,
    val Name: String,
    val VersionId: VersionId? = null,
    val VersionStages: List<String>? = null
)
