package org.http4k.connect.amazon.secretsmanager.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.core.model.KMSKeyId
import org.http4k.connect.amazon.core.model.Tag
import org.http4k.connect.amazon.secretsmanager.SecretsManagerAction
import org.http4k.connect.amazon.secretsmanager.model.VersionId
import org.http4k.connect.model.Base64Blob
import se.ansman.kotshi.JsonSerializable
import java.util.UUID

@Http4kConnectAction
@JsonSerializable
@ExposedCopyVisibility
data class CreateSecret internal constructor(
    val Name: String,
    val ClientRequestToken: UUID,
    val SecretString: String? = null,
    val SecretBinary: Base64Blob? = null,
    val Description: String? = null,
    val KmsKeyId: KMSKeyId? = null,
    val Tags: List<Tag>? = null
) : SecretsManagerAction<CreatedSecret>(CreatedSecret::class) {
    constructor(
        Name: String,
        ClientRequestToken: UUID,
        SecretString: String,
        Description: String? = null,
        KmsKeyId: KMSKeyId? = null,
        Tags: List<Tag>? = null
    ) : this(Name, ClientRequestToken, SecretString, null, Description, KmsKeyId, Tags)

    constructor(
        Name: String,
        ClientRequestToken: UUID,
        SecretBinary: Base64Blob,
        Description: String? = null,
        KmsKeyId: KMSKeyId? = null,
        Tags: List<Tag>? = null
    ) : this(Name, ClientRequestToken, null, SecretBinary, Description, KmsKeyId, Tags)
}

@JsonSerializable
data class CreatedSecret(
    val ARN: ARN,
    val Name: String,
    val VersionId: VersionId? = null
)
