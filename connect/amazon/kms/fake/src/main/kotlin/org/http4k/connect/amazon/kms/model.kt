package org.http4k.connect.amazon.kms

import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.core.model.KMSKeyId
import org.http4k.connect.amazon.kms.model.CustomerMasterKeySpec
import org.http4k.connect.amazon.kms.model.KeyUsage
import org.http4k.connect.amazon.kms.model.SigningAlgorithm
import org.http4k.connect.model.Base64Blob
import org.http4k.connect.model.Timestamp
import java.security.KeyFactory
import java.security.Provider
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec

data class StoredCMK(
    val keyId: KMSKeyId,
    val arn: ARN,
    val keyUsage: KeyUsage,
    val keySpec: CustomerMasterKeySpec,
    val privateKeyContent: EncryptionKeyContent?,
    val publicKeyContent: EncryptionKeyContent?,
    val deletion: Timestamp? = null
)

data class EncryptionKeyContent(
    val format: String,
    val encoded: Base64Blob
)

val EncryptionKeyContent.keySpec
    get() = when (format) {
        "PKCS#8" -> PKCS8EncodedKeySpec(encoded.decodedBytes())
        "X.509" -> X509EncodedKeySpec(encoded.decodedBytes())
        else -> error("Unsupported format: $format")
    }

val StoredCMK.signingAlgorithms
    get() = when (keySpec) {
        CustomerMasterKeySpec.RSA_2048, CustomerMasterKeySpec.RSA_3072, CustomerMasterKeySpec.RSA_4096 -> listOf(
            SigningAlgorithm.RSASSA_PKCS1_V1_5_SHA_256,
            SigningAlgorithm.RSASSA_PKCS1_V1_5_SHA_384,
            SigningAlgorithm.RSASSA_PKCS1_V1_5_SHA_512,
            SigningAlgorithm.RSASSA_PSS_SHA_256,
            SigningAlgorithm.RSASSA_PSS_SHA_384,
            SigningAlgorithm.RSASSA_PSS_SHA_512
        )

        CustomerMasterKeySpec.ECC_NIST_P256, CustomerMasterKeySpec.ECC_NIST_P384, CustomerMasterKeySpec.ECC_NIST_P521 -> listOf(
            SigningAlgorithm.ECDSA_SHA_256,
            SigningAlgorithm.ECDSA_SHA_384,
            SigningAlgorithm.ECDSA_SHA_512
        )

        CustomerMasterKeySpec.ECC_SECG_P256K1 -> emptyList()
        CustomerMasterKeySpec.SYMMETRIC_DEFAULT -> emptyList()
    }

private fun StoredCMK.keyFactory(crypto: Provider) = when (keySpec) {
    CustomerMasterKeySpec.RSA_2048, CustomerMasterKeySpec.RSA_3072, CustomerMasterKeySpec.RSA_4096 -> KeyFactory.getInstance(
        "RSA",
        crypto
    )

    CustomerMasterKeySpec.ECC_NIST_P256, CustomerMasterKeySpec.ECC_NIST_P384, CustomerMasterKeySpec.ECC_NIST_P521 -> KeyFactory.getInstance(
        "ECDSA",
        crypto
    )

    CustomerMasterKeySpec.ECC_SECG_P256K1 -> null
    CustomerMasterKeySpec.SYMMETRIC_DEFAULT -> null
}

fun StoredCMK.loadPublicKey(crypto: Provider) = publicKeyContent?.keySpec
    ?.let { keyFactory(crypto)?.generatePublic(it) }

fun StoredCMK.loadPrivateKey(crypto: Provider) = privateKeyContent?.keySpec
    ?.let { keyFactory(crypto)?.generatePrivate(it) }
