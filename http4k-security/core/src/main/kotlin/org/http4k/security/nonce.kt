package org.http4k.security

import java.math.BigInteger
import java.security.SecureRandom

data class Nonce(val value: String) {
    companion object {
        val SECURE_NONCE = NonceGenerator { Nonce(BigInteger(130, SecureRandom()).toString(32)) }
    }

    override fun toString(): String = value
}

fun interface NonceGenerator {
    operator fun invoke(): Nonce
}

fun interface NonceVerifier {
    operator fun invoke(nonce: Nonce): Boolean
}

interface NonceGeneratorVerifier : NonceVerifier, NonceGenerator
