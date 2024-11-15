package org.http4k.connect.amazon.kms.model

import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.core.model.AwsAccount
import org.http4k.connect.amazon.core.model.KMSKeyId
import org.http4k.connect.model.Timestamp
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class KeyMetadata(
    val KeyId: KMSKeyId,
    val Arn: ARN? = null,
    val AWSAccountId: AwsAccount? = null,
    val KeyUsage: KeyUsage? = null,
    val EncryptionAlgorithms: List<EncryptionAlgorithm>? = null,
    val SigningAlgorithms: List<SigningAlgorithm>? = null,
    val CustomerMasterKeySpec: CustomerMasterKeySpec? = null,
    val Enabled: Boolean? = null,
    val CreationDate: Timestamp? = null,
    val CloudHsmClusterId: String? = null,
    val CustomKeyStoreId: String? = null,
    val DeletionDate: Timestamp? = null,
    val Description: String? = null,
    val ExpirationModel: String? = null,
    val KeyManager: String? = null,
    val KeyState: String? = null,
    val Origin: String? = null,
    val ValidTo: Timestamp? = null
)
