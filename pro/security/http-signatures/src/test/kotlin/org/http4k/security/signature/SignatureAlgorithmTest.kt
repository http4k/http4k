package org.http4k.security.signature

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.startsWith
import org.junit.jupiter.api.Test
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey

class SignatureAlgorithmTest {

    @Test
    fun `signature encoding and decoding work correctly`() {
        val rawSignature = "TestSignature".toByteArray()
        val encoded = RsaPssSha512.encodeSignature(rawSignature)

        assertThat(encoded, startsWith(":"))
        assertThat(encoded.endsWith(":"), equalTo(true))

        val decoded = RsaPssSha512.decodeSignature(encoded)
        assertThat(decoded.contentEquals(rawSignature), equalTo(true))
    }

    @Test
    fun `handle edge cases correctly`() {
        val emptySignature = RsaPssSha512.encodeSignature(ByteArray(0))
        assertThat(emptySignature, startsWith(":"))
        assertThat(emptySignature.endsWith(":"), equalTo(true))
        assertThat(RsaPssSha512.decodeSignature(emptySignature).isEmpty(), equalTo(true))

        val rawSignature = "TestSignature".toByteArray()
        val encoded = RsaPssSha512.encodeSignature(rawSignature)
        val encodedWithoutColons = encoded.substring(1, encoded.length - 1)
        assertThat(RsaPssSha512.decodeSignature(encodedWithoutColons).contentEquals(rawSignature), equalTo(true))
    }

    @Test
    fun `RsaPssSha512 can sign and verify`() {
        val keyPair = generateRsaKeyPair()
        val signatureBase = "Test message for RSA-PSS-SHA512 signing"

        val signature = RsaPssSha512.sign(signatureBase, keyPair.private)

        assertThat(signature, startsWith(":"))
        assertThat(signature.endsWith(":"), equalTo(true))

        assertThat(RsaPssSha512.verify(signatureBase, signature, keyPair.public), equalTo(true))

        val differentKeyPair = generateRsaKeyPair()
        assertThat(RsaPssSha512.verify(signatureBase, signature, differentKeyPair.public), equalTo(false))

        assertThat(RsaPssSha512.verify("Tampered message", signature, keyPair.public), equalTo(false))
    }

    @Test
    fun `RsaPkcs1Sha256 can sign and verify`() {
        val keyPair = generateRsaKeyPair()
        val signatureBase = "Test message for RSA-PKCS1-SHA256 signing"

        val signature = RsaPkcs1Sha256.sign(signatureBase, keyPair.private)

        assertThat(signature, startsWith(":"))
        assertThat(signature.endsWith(":"), equalTo(true))

        assertThat(RsaPkcs1Sha256.verify(signatureBase, signature, keyPair.public), equalTo(true))

        val differentKeyPair = generateRsaKeyPair()
        assertThat(RsaPkcs1Sha256.verify(signatureBase, signature, differentKeyPair.public), equalTo(false))

        assertThat(RsaPkcs1Sha256.verify("Tampered message", signature, keyPair.public), equalTo(false))
    }

    @Test
    fun `HmacSha256 can sign and verify with ByteArray`() {
        val key = "test-secret-key".toByteArray()
        val signatureBase = "Test message for HMAC-SHA256 signing"

        val signature = HmacSha256.sign(signatureBase, key)

        assertThat(signature, startsWith(":"))
        assertThat(signature.endsWith(":"), equalTo(true))

        assertThat(HmacSha256.verify(signatureBase, signature, key), equalTo(true))

        val differentKey = "different-key".toByteArray()
        assertThat(HmacSha256.verify(signatureBase, signature, differentKey), equalTo(false))

        assertThat(HmacSha256.verify("Tampered message", signature, key), equalTo(false))
    }

    @Test
    fun `EcdsaP256Sha256 can sign and verify`() {
        val keyPair = generateEcKeyPair()
        val signatureBase = "Test message for ECDSA-P256-SHA256 signing"

        val signature = EcdsaP256Sha256.sign(signatureBase, keyPair.private)

        assertThat(signature, startsWith(":"))
        assertThat(signature.endsWith(":"), equalTo(true))

        assertThat(EcdsaP256Sha256.verify(signatureBase, signature, keyPair.public), equalTo(true))

        val differentKeyPair = generateEcKeyPair()
        assertThat(EcdsaP256Sha256.verify(signatureBase, signature, differentKeyPair.public), equalTo(false))

        assertThat(EcdsaP256Sha256.verify("Tampered message", signature, keyPair.public), equalTo(false))
    }

    @Test
    fun `Ed25519 can sign and verify`() {
        val keyPair = generateEdDsaKeyPair()
        val signatureBase = "Test message for Ed25519 signing"

        val signature = Ed25519.sign(signatureBase, keyPair.private)

        assertThat(signature, startsWith(":"))
        assertThat(signature.endsWith(":"), equalTo(true))

        assertThat(Ed25519.verify(signatureBase, signature, keyPair.public), equalTo(true))

        val differentKeyPair = generateEdDsaKeyPair()
        assertThat(Ed25519.verify(signatureBase, signature, differentKeyPair.public), equalTo(false))

        assertThat(Ed25519.verify("Tampered message", signature, keyPair.public), equalTo(false))
    }

    private fun generateRsaKeyPair(): KeyPair {
        val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
        keyPairGenerator.initialize(2048, SecureRandom())
        val javaKeyPair = keyPairGenerator.generateKeyPair()

        return KeyPair(
            javaKeyPair.public as RSAPublicKey,
            javaKeyPair.private as RSAPrivateKey
        )
    }

    private fun generateEcKeyPair(): KeyPair {
        val keyPairGenerator = KeyPairGenerator.getInstance("EC")
        keyPairGenerator.initialize(256, SecureRandom())
        val javaKeyPair = keyPairGenerator.generateKeyPair()

        return KeyPair(
            javaKeyPair.public as ECPublicKey,
            javaKeyPair.private as ECPrivateKey
        )
    }

    private fun generateEdDsaKeyPair(): KeyPair {
        val keyPairGenerator = KeyPairGenerator.getInstance("Ed25519")
        val javaKeyPair = keyPairGenerator.generateKeyPair()

        return KeyPair(
            javaKeyPair.public,
            javaKeyPair.private
        )
    }

    data class KeyPair(val public: java.security.PublicKey, val private: java.security.PrivateKey)
}
