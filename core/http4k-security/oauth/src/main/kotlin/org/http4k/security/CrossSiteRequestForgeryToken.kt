package org.http4k.security

import org.http4k.core.Request
import java.math.BigInteger
import java.security.SecureRandom

data class CrossSiteRequestForgeryToken(val value: String) {
    companion object {
        val SECURE_CSRF = { _: Request -> CrossSiteRequestForgeryToken(BigInteger(130, SecureRandom()).toString(32)) }
    }
}

typealias CsrfGenerator = (Request) -> CrossSiteRequestForgeryToken
