package org.http4k.security.signature

import org.http4k.security.Nonce
import java.time.Instant

data class SignatureParameters(
    val keyId: KeyId,
    val algorithm: SignatureAlgorithmName,
    val created: Instant? = null,
    val expires: Instant? = null,
    val nonce: Nonce? = null,
    val tag: String? = null
)
