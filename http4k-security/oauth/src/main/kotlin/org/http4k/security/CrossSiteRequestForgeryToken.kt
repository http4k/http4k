package org.http4k.security

import java.math.BigInteger
import java.security.SecureRandom

data class CrossSiteRequestForgeryToken(val value: String) {
    companion object {
        val SECURE_CSRF = { CrossSiteRequestForgeryToken(BigInteger(130, SecureRandom()).toString(32)) }
    }
}

typealias CsrfGenerator = () -> CrossSiteRequestForgeryToken
