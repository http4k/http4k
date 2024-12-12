package org.http4k.connect.amazon.kms

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.endsWith
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.present
import dev.forkhandles.result4k.failureOrNull
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.http4k.connect.amazon.AwsContract
import org.http4k.connect.amazon.core.model.KMSKeyId
import org.http4k.connect.amazon.core.model.toARN
import org.http4k.connect.amazon.kms.model.CustomerMasterKeySpec.ECC_NIST_P521
import org.http4k.connect.amazon.kms.model.CustomerMasterKeySpec.RSA_2048
import org.http4k.connect.amazon.kms.model.CustomerMasterKeySpec.RSA_3072
import org.http4k.connect.amazon.kms.model.EncryptionAlgorithm.RSAES_OAEP_SHA_256
import org.http4k.connect.amazon.kms.model.KeyUsage.ENCRYPT_DECRYPT
import org.http4k.connect.amazon.kms.model.KeyUsage.SIGN_VERIFY
import org.http4k.connect.amazon.kms.model.SigningAlgorithm.ECDSA_SHA_512
import org.http4k.connect.amazon.kms.model.SigningAlgorithm.RSASSA_PKCS1_V1_5_SHA_256
import org.http4k.connect.amazon.kms.model.SigningAlgorithm.RSASSA_PSS_SHA_256
import org.http4k.connect.model.Base64Blob
import org.http4k.connect.successValue
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.junit.jupiter.api.Test
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec


interface KMSContract : AwsContract {
    private val kms get() = KMS.Http(aws.region, { aws.credentials }, http)

    @Test
    fun `encrypt-decrypt key lifecycle`() {
        val plaintextString = Base64Blob.encode("hello there")
        val plaintextBinary = Base64Blob.of("GiD3+7WHA+0nJYnGIB3E25tm5rJqwlYi2IpLzEDRqE0ymw==")

        val creation = kms.createKey(RSA_3072, ENCRYPT_DECRYPT).successValue()

        val keyId = creation.KeyMetadata.KeyId
        assertThat(keyId, present())

        assertThat(kms.listKeys().successValue().Keys.any { it.KeyId == keyId }, equalTo(true))

        try {
            val describe = kms.describeKey(keyId).successValue()
            assertThat(describe.KeyMetadata.KeyId, equalTo(keyId))

            val encryptText = kms.encrypt(keyId, plaintextString, RSAES_OAEP_SHA_256).successValue()
            assertThat(encryptText.KeyId.toARN().value, endsWith(keyId.value))

            val decryptText = kms.decrypt(keyId, encryptText.CiphertextBlob, RSAES_OAEP_SHA_256).successValue()
            assertThat(decryptText.KeyId.toARN().value, endsWith(keyId.value))
            assertThat(decryptText.Plaintext, equalTo(plaintextString))

            val encryptBinary = kms.encrypt(keyId, plaintextBinary, RSAES_OAEP_SHA_256).successValue()
            assertThat(encryptBinary.KeyId.toARN().value, endsWith(keyId.value))

            val decryptBinary = kms.decrypt(keyId, encryptBinary.CiphertextBlob, RSAES_OAEP_SHA_256).successValue()
            assertThat(decryptBinary.KeyId.toARN().value, endsWith(keyId.value))
            assertThat(decryptBinary.Plaintext, equalTo(plaintextBinary))

            val publicKey = kms.getPublicKey(keyId).successValue()
            assertThat(publicKey.KeyId.toARN().value, endsWith(keyId.value))
        } finally {
            val deletion = kms.scheduleKeyDeletion(keyId, 7).successValue()
            assertThat(deletion.KeyId.toARN().value, endsWith(keyId.value))
        }
    }

    @Test
    fun `RSA sign-verify key lifecycle`() {
        val message1 = Base64Blob.encode("hello there")
        val message2 = Base64Blob.encode("goodbye there")

        val creation = kms.createKey(RSA_3072, SIGN_VERIFY).successValue()
        val keyId = creation.KeyMetadata.KeyId
        assertThat(keyId, present())

        try {
            val describe = kms.describeKey(keyId).successValue()
            assertThat(describe.KeyMetadata.KeyId, equalTo(keyId))

            val signed = kms.sign(keyId, message1, RSASSA_PSS_SHA_256).successValue()
            assertThat(signed.SigningAlgorithm, equalTo(RSASSA_PSS_SHA_256))

            val verification = kms.verify(keyId, message1, signed.Signature, RSASSA_PSS_SHA_256).successValue()
            assertThat(verification.SignatureValid, equalTo(true))

            val verificationFailure =
                kms.verify(keyId, message2, signed.Signature, RSASSA_PSS_SHA_256).failureOrNull()
            assertThat(verificationFailure!!.status, equalTo(BAD_REQUEST))
        } finally {
            val deletion = kms.scheduleKeyDeletion(keyId, 7).successValue()
            assertThat(deletion.KeyId.toARN().value, endsWith(keyId.value))
        }
    }

    @Test
    fun `ECDSA sign-verify key lifecycle`() {
        val message1 = Base64Blob.encode("hello there")
        val message2 = Base64Blob.encode("General Kenobi!")

        val creation = kms.createKey(ECC_NIST_P521, SIGN_VERIFY).successValue()
        val keyId = creation.KeyMetadata.KeyId
        assertThat(keyId, present())

        try {
            val describe = kms.describeKey(keyId).successValue()
            assertThat(describe.KeyMetadata.KeyId, equalTo(keyId))

            val signed = kms.sign(keyId, message1, ECDSA_SHA_512).successValue()
            assertThat(signed.SigningAlgorithm, equalTo(ECDSA_SHA_512))

            val verification = kms.verify(keyId, message1, signed.Signature, ECDSA_SHA_512).successValue()
            assertThat(verification.SignatureValid, equalTo(true))

            val verificationFailure =
                kms.verify(keyId, message2, signed.Signature, ECDSA_SHA_512).failureOrNull()
            assertThat(verificationFailure!!.status, equalTo(BAD_REQUEST))
        } finally {
            val deletion = kms.scheduleKeyDeletion(keyId, 7).successValue()
            assertThat(deletion.KeyId.toARN().value, endsWith(keyId.value))
        }
    }

    @Test
    fun `retrieve RSA key for signing`() {
        val message1 = Base64Blob.encode("hello there")

        val creation = kms.createKey(RSA_2048, SIGN_VERIFY).successValue()
        val keyId = creation.KeyMetadata.KeyId
        assertThat(keyId, present())

        try {
            val signed = kms.sign(keyId, message1, RSASSA_PKCS1_V1_5_SHA_256).successValue()

            assertThat(signed.SigningAlgorithm, equalTo(RSASSA_PKCS1_V1_5_SHA_256))

            val publicKey = kms.getPublicKey(keyId).successValue().PublicKey

            val keySpec = X509EncodedKeySpec(publicKey.decodedBytes())

            assertThat(
                Signature.getInstance("SHA256withRSA").apply {
                    initVerify(KeyFactory.getInstance("RSA").generatePublic(keySpec))
                    update(message1.decodedBytes())
                }.verify(signed.Signature.decodedBytes()), equalTo(true)
            )

        } finally {
            val deletion = kms.scheduleKeyDeletion(keyId, 7).successValue()
            assertThat(deletion.KeyId.toARN().value, endsWith(keyId.value))
        }
    }

    @Test
    fun `retrieve ECDSA key for signing`() {
        val message1 = Base64Blob.encode("hello there")

        val creation = kms.createKey(ECC_NIST_P521, SIGN_VERIFY).successValue()
        val keyId = creation.KeyMetadata.KeyId
        assertThat(keyId, present())

        try {
            val signed = kms.sign(keyId, message1, ECDSA_SHA_512).successValue()

            assertThat(signed.SigningAlgorithm, equalTo(ECDSA_SHA_512))

            val publicKeyContent = kms.getPublicKey(keyId).successValue().PublicKey

            val keySpec = X509EncodedKeySpec(publicKeyContent.decodedBytes())
            val publicKey = KeyFactory.getInstance("ECDSA", BouncyCastleProvider()).generatePublic(keySpec)

            assertThat(
                Signature.getInstance("SHA512withECDSA", BouncyCastleProvider()).apply {
                    initVerify(publicKey)
                    update(message1.decodedBytes())
                }.verify(signed.Signature.decodedBytes()), equalTo(true)
            )

        } finally {
            val deletion = kms.scheduleKeyDeletion(keyId, 7).successValue()
            assertThat(deletion.KeyId.toARN().value, endsWith(keyId.value))
        }
    }

    @Test
    fun `verify with missing key`() {
        val keyId = kms.createKey(RSA_2048, SIGN_VERIFY).successValue().KeyMetadata.KeyId

        try {
            val message1 = Base64Blob.encode("General Kenobi!")
            val signed = kms.sign(keyId, message1, RSASSA_PKCS1_V1_5_SHA_256).successValue()

            val verificationFailure =
                kms.verify(KMSKeyId.of("missingKey"), message1, signed.Signature, RSASSA_PKCS1_V1_5_SHA_256)
                    .failureOrNull()
            assertThat(verificationFailure!!.status, equalTo(BAD_REQUEST))
        } finally {
            kms.scheduleKeyDeletion(keyId, 7).successValue()
        }
    }

    @Test
    fun `key cannot verify signature of another key`() {
        val key1 = kms.createKey(RSA_2048, SIGN_VERIFY).successValue().KeyMetadata.KeyId
        val key2 = kms.createKey(RSA_2048, SIGN_VERIFY).successValue().KeyMetadata.KeyId

        try {
            val message1 = Base64Blob.encode("General Kenobi!")

            val signed = kms.sign(key1, message1, RSASSA_PKCS1_V1_5_SHA_256).successValue()

            val verificationFailure =
                kms.verify(key2, message1, signed.Signature, RSASSA_PKCS1_V1_5_SHA_256).failureOrNull()
            assertThat(verificationFailure!!.status, equalTo(BAD_REQUEST))
        } finally {
            kms.scheduleKeyDeletion(key1, 7)
            kms.scheduleKeyDeletion(key2, 7).successValue()
        }
    }

    @Test
    fun `create key with new KeySpec property`() {
        val keyId = kms.createKey(KeyUsage = SIGN_VERIFY, KeySpec = RSA_3072).successValue().KeyMetadata.KeyId

        try {
            assertThat(kms.getPublicKey(keyId).successValue().KeySpec, equalTo(RSA_3072))
        } finally {
            kms.scheduleKeyDeletion(keyId, 7).successValue()
        }
    }
}
