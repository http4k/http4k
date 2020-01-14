package org.http4k.security.openid

import java.math.BigInteger
import java.security.SecureRandom

data class Nonce(val value: String) {
    companion object {
        val SECURE_NONCE = { Nonce(BigInteger(130, SecureRandom()).toString(32)) }
    }
}

typealias NonceGenerator = () -> Nonce
