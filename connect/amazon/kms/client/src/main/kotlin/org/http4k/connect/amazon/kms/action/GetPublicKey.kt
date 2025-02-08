package org.http4k.connect.amazon.kms.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.core.model.KMSKeyId
import org.http4k.connect.amazon.kms.KMSAction
import org.http4k.connect.amazon.kms.model.CustomerMasterKeySpec
import org.http4k.connect.amazon.kms.model.EncryptionAlgorithm
import org.http4k.connect.amazon.kms.model.KeyUsage
import org.http4k.connect.amazon.kms.model.SigningAlgorithm
import org.http4k.connect.model.Base64Blob
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
@JsonSerializable
data class GetPublicKey(
    val KeyId: KMSKeyId,
    val GrantTokens: List<String>? = null
) : KMSAction<PublicKey>(PublicKey::class)

@JsonSerializable
data class PublicKey(
    val KeyId: KMSKeyId,
    val KeySpec: CustomerMasterKeySpec,
    val KeyUsage: KeyUsage,
    val PublicKey: Base64Blob,
    val EncryptionAlgorithms: List<EncryptionAlgorithm>? = null,
    val SigningAlgorithms: List<SigningAlgorithm>? = null
)
