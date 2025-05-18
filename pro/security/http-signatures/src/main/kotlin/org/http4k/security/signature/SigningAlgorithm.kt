package org.http4k.security.signature

import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature
import java.security.spec.MGF1ParameterSpec
import java.security.spec.PSSParameterSpec
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * A signature algorithm used for signing and verifying signatures.
 *
 * @param PrivateKey the type of the private key used for signing.
 * @param PublicKey the type of the public key used for verifying.
 */
interface SignatureAlgorithm<PrivateKey, PublicKey> {
    val name: SignatureAlgorithmName

    fun sign(signatureBase: String, privateKey: PrivateKey): SignatureValue

    fun verify(signatureBase: String, signature: SignatureValue, publicKey: PublicKey): Boolean

    fun encodeSignature(rawSignature: ByteArray): SignatureValue =
        ":${Base64.getEncoder().encodeToString(rawSignature)}:"

    fun decodeSignature(encodedSignature: SignatureValue): ByteArray =
        Base64.getDecoder().decode(encodedSignature.trim(':'))
}

object RsaPssSha512 : SignatureAlgorithm<PrivateKey, PublicKey> {
    override val name = "rsa-pss-sha512"
    private val pssParams = PSSParameterSpec("SHA-512", "MGF1", MGF1ParameterSpec("SHA-512"), 64, 1)

    override fun sign(signatureBase: String, privateKey: PrivateKey): SignatureValue =
        encodeSignature(Signature.getInstance("RSASSA-PSS").apply {
            setParameter(pssParams)
            initSign(privateKey)
            update(signatureBase.toByteArray())
        }.sign())

    override fun verify(signatureBase: String, signature: SignatureValue, publicKey: PublicKey): Boolean =
        Signature.getInstance("RSASSA-PSS").apply {
            setParameter(pssParams)
            initVerify(publicKey)
            update(signatureBase.toByteArray())
        }.verify(decodeSignature(signature))
}

object RsaPkcs1Sha256 : SignatureAlgorithm<PrivateKey, PublicKey> {
    override val name = "rsa-v1_5-sha256"

    override fun sign(signatureBase: String, privateKey: PrivateKey): SignatureValue =
        encodeSignature(Signature.getInstance("SHA256withRSA").apply {
            initSign(privateKey)
            update(signatureBase.toByteArray())
        }.sign())

    override fun verify(signatureBase: String, signature: SignatureValue, publicKey: PublicKey): Boolean =
        Signature.getInstance("SHA256withRSA").apply {
            initVerify(publicKey)
            update(signatureBase.toByteArray())
        }.verify(decodeSignature(signature))
}

object HmacSha256 : SignatureAlgorithm<ByteArray, ByteArray> {
    override val name = "hmac-sha256"

    override fun sign(signatureBase: String, privateKey: ByteArray): SignatureValue =
        encodeSignature(Mac.getInstance("HmacSHA256").apply {
            init(SecretKeySpec(privateKey, "HmacSHA256"))
        }.doFinal(signatureBase.toByteArray()))

    override fun verify(signatureBase: String, signature: SignatureValue, publicKey: ByteArray): Boolean =
        sign(signatureBase, publicKey) == signature
}

object EcdsaP256Sha256 : SignatureAlgorithm<PrivateKey, PublicKey> {
    override val name = "ecdsa-p256-sha256"

    override fun sign(signatureBase: String, privateKey: PrivateKey): SignatureValue =
        encodeSignature(Signature.getInstance("SHA256withECDSA").apply {
            initSign(privateKey)
            update(signatureBase.toByteArray())
        }.sign())

    override fun verify(signatureBase: String, signature: SignatureValue, publicKey: PublicKey): Boolean =
        Signature.getInstance("SHA256withECDSA").apply {
            initVerify(publicKey)
            update(signatureBase.toByteArray())
        }.verify(decodeSignature(signature))
}

object Ed25519 : SignatureAlgorithm<PrivateKey, PublicKey> {
    override val name = "ed25519"

    override fun sign(signatureBase: String, privateKey: PrivateKey): SignatureValue =
        encodeSignature(Signature.getInstance("Ed25519").apply {
            initSign(privateKey)
            update(signatureBase.toByteArray())
        }.sign())

    override fun verify(signatureBase: String, signature: SignatureValue, publicKey: PublicKey): Boolean =
        Signature.getInstance("Ed25519").apply {
            initVerify(publicKey)
            update(signatureBase.toByteArray())
        }.verify(decodeSignature(signature))
}
