package org.http4k.connect.amazon.kms

import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.http4k.connect.amazon.kms.model.SigningAlgorithm.ECDSA_SHA_256
import org.http4k.connect.amazon.kms.model.SigningAlgorithm.ECDSA_SHA_384
import org.http4k.connect.amazon.kms.model.SigningAlgorithm.ECDSA_SHA_512
import org.http4k.connect.amazon.kms.model.SigningAlgorithm.RSASSA_PKCS1_V1_5_SHA_256
import org.http4k.connect.amazon.kms.model.SigningAlgorithm.RSASSA_PKCS1_V1_5_SHA_384
import org.http4k.connect.amazon.kms.model.SigningAlgorithm.RSASSA_PKCS1_V1_5_SHA_512
import org.http4k.connect.amazon.kms.model.SigningAlgorithm.RSASSA_PSS_SHA_256
import org.http4k.connect.amazon.kms.model.SigningAlgorithm.RSASSA_PSS_SHA_384
import org.http4k.connect.amazon.kms.model.SigningAlgorithm.RSASSA_PSS_SHA_512
import org.http4k.connect.model.Base64Blob
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature
import java.security.spec.MGF1ParameterSpec
import java.security.spec.MGF1ParameterSpec.SHA256
import java.security.spec.MGF1ParameterSpec.SHA384
import java.security.spec.MGF1ParameterSpec.SHA512
import java.security.spec.PSSParameterSpec


sealed class KMSSigningAlgorithm(val javaAlgo: String) {
    abstract fun verify(key: PublicKey, message: Base64Blob, signature: Base64Blob): Boolean

    abstract fun sign(key: PrivateKey, message: Base64Blob): Base64Blob

    companion object {
        val KMS_ALGORITHMS = mapOf(
            RSASSA_PSS_SHA_256 to RSA_PSS("SHA256withRSA/PSS", SHA256, 32, "SHA-256"),
            RSASSA_PSS_SHA_384 to RSA_PSS("SHA384withRSA/PSS", SHA384, 48, "SHA-384"),
            RSASSA_PSS_SHA_512 to RSA_PSS("SHA512withRSA/PSS", SHA512, 64, "SHA-512"),
            RSASSA_PKCS1_V1_5_SHA_256 to RSA_PCKS1_V1_5("SHA256withRSA"),
            RSASSA_PKCS1_V1_5_SHA_384 to RSA_PCKS1_V1_5("SHA384withRSA"),
            RSASSA_PKCS1_V1_5_SHA_512 to RSA_PCKS1_V1_5("SHA512withRSA"),
            ECDSA_SHA_256 to ECDSA("SHA256withECDSA"),
            ECDSA_SHA_384 to ECDSA("SHA384withECDSA"),
            ECDSA_SHA_512 to ECDSA("SHA512withECDSA"),
        )
    }
}


class RSA_PSS(
    algo: String,
    private val mgf: MGF1ParameterSpec,
    private val saltLength: Int,
    private val parameterAlgorithm: String
) : KMSSigningAlgorithm(algo) {

    private val crypto = BouncyCastleProvider()

    override fun verify(key: PublicKey, message: Base64Blob, signature: Base64Blob) =
        Signature.getInstance(javaAlgo, crypto).run {
            initVerify(key)
            update(message.decodedBytes())
            verify(signature.decodedBytes())
        }

    override fun sign(key: PrivateKey, message: Base64Blob) = Base64Blob.encode(
        Signature.getInstance(javaAlgo, crypto).run {
            initSign(key)
            setParameter(PSSParameterSpec(parameterAlgorithm, "MGF1", mgf, saltLength, 1))
            update(message.decodedBytes())
            sign()
        })
}

class RSA_PCKS1_V1_5(algo: String) : KMSSigningAlgorithm(algo) {

    private val crypto = BouncyCastleProvider()

    override fun verify(key: PublicKey, message: Base64Blob, signature: Base64Blob) =
        Signature.getInstance(javaAlgo, crypto).run {
            initVerify(key)
            update(message.decodedBytes())
            verify(signature.decodedBytes())
        }

    override fun sign(key: PrivateKey, message: Base64Blob) = Base64Blob.encode(
        Signature.getInstance(javaAlgo, crypto).run {
            initSign(key)
            update(message.decodedBytes())
            sign()
        })

}

class ECDSA(algo: String) : KMSSigningAlgorithm(algo) {
    private val crypto = BouncyCastleProvider()

    override fun verify(key: PublicKey, message: Base64Blob, signature: Base64Blob) =
        Signature.getInstance(javaAlgo, crypto).run {
            initVerify(key)
            update(message.decodedBytes())
            try {
                verify(signature.decodedBytes())
            } catch (e: Exception) {
                false
            }
        }

    override fun sign(key: PrivateKey, message: Base64Blob) = Base64Blob.encode(
        Signature.getInstance(javaAlgo, crypto).run {
            initSign(key)
            update(message.decodedBytes())
            sign()
        })

}
