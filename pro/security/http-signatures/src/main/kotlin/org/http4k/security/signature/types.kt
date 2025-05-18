package org.http4k.security.signature

import org.http4k.core.HttpMessage
import org.http4k.security.Nonce
import java.time.Instant

typealias KeyId = String
typealias SignatureAlgorithmName = String
typealias SignatureValue = String
typealias ComponentValue = String

/**
 * The makeup of a signature input in the HTTP Signature header.
 */
data class SignatureInput<Target : HttpMessage>(
    val label: String,
    val components: List<SignatureComponent<Target>>,
    val parameters: SignatureParameters
)

data class SignatureParameters(
    val keyId: KeyId,
    val algorithm: SignatureAlgorithmName,
    val created: Instant? = null,
    val expires: Instant? = null,
    val nonce: Nonce? = null,
    val tag: String? = null
)

data class Signature(val label: String, val value: SignatureValue)
