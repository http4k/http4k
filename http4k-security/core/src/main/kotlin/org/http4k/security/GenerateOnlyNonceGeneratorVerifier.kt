package org.http4k.security

import org.http4k.util.Hex
import java.security.SecureRandom

class GenerateOnlyNonceGeneratorVerifier : NonceGeneratorVerifier {
    private val random = SecureRandom()

    override fun invoke(nonce: Nonce): Boolean = true

    override fun invoke(): Nonce {
        val tmp = ByteArray(length)
        random.nextBytes(tmp)
        return Nonce(Hex.hex(tmp))
    }

    companion object {
        private const val length = 8
    }
}

/**
 * TODO [SecureRandomNonceGenerator] cannot verify nonces.
 * Come up with an implementation that can, such as Ktor's StatelessHmacNonceManager,
 * or one that uses an expiring cache for secure random nonces
 */

