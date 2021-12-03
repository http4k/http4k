package org.http4k.security

import java.math.BigInteger
import java.security.SecureRandom

data class Nonce(val value: String) {
    companion object {
        val SECURE_NONCE = { Nonce(BigInteger(130, SecureRandom()).toString(32)) }
    }

    override fun toString(): String = value
}

typealias NonceGenerator = () -> Nonce
typealias NonceVerifier = (Nonce) -> Boolean
