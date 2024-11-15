package org.http4k.connect.amazon.kms.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.core.model.KMSKeyId
import org.http4k.connect.amazon.kms.KMSAction
import org.http4k.connect.amazon.kms.model.EncryptionAlgorithm
import org.http4k.connect.model.Base64Blob
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
@JsonSerializable
data class Decrypt(
    val KeyId: KMSKeyId,
    val CiphertextBlob: Base64Blob,
    val EncryptionAlgorithm: EncryptionAlgorithm? = null,
    val EncryptionContext: Map<String, String>? = null,
    val GrantTokens: List<String>? = null
) : KMSAction<Decrypted>(Decrypted::class)

@JsonSerializable
data class Decrypted(val KeyId: KMSKeyId, val Plaintext: Base64Blob, val EncryptionAlgorithm: EncryptionAlgorithm)
