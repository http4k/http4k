package org.http4k.connect.amazon.kms

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.http4k.connect.amazon.kms.KMSSigningAlgorithm.Companion.KMS_ALGORITHMS
import org.http4k.connect.amazon.kms.model.SigningAlgorithm
import org.http4k.connect.model.Base64Blob
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.security.KeyPairGenerator

class KMSSigningAlgorithmTest {

    @ParameterizedTest
    @MethodSource("algorithms")
    fun `can sign and verify`(algorithm: Pair<SigningAlgorithm, KMSSigningAlgorithm>) {
        val keypair = algorithm.first.genKeyPair()

        val message = Base64Blob.encode("foobar")
        val signature = algorithm.second.sign(keypair.private, message)

        assertThat(algorithm.second.verify(keypair.public, message, signature), equalTo(true))

        val invalid = Base64Blob.encode(signature.decodedBytes().reversed().toByteArray())
        assertThat(algorithm.second.verify(keypair.public, message, invalid), equalTo(false))
    }

    companion object {
        @JvmStatic
        fun algorithms() = KMS_ALGORITHMS.toList()
    }
}

private fun SigningAlgorithm.genKeyPair() = when (this) {
    SigningAlgorithm.RSASSA_PSS_SHA_256, SigningAlgorithm.RSASSA_PSS_SHA_384, SigningAlgorithm.RSASSA_PSS_SHA_512,
    SigningAlgorithm.RSASSA_PKCS1_V1_5_SHA_256, SigningAlgorithm.RSASSA_PKCS1_V1_5_SHA_384,
    SigningAlgorithm.RSASSA_PKCS1_V1_5_SHA_512 -> {
        KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
    }

    SigningAlgorithm.ECDSA_SHA_256, SigningAlgorithm.ECDSA_SHA_384, SigningAlgorithm.ECDSA_SHA_512 -> {
        KeyPairGenerator.getInstance("ECDSA", BouncyCastleProvider()).apply { initialize(521) }.generateKeyPair()
    }
}
