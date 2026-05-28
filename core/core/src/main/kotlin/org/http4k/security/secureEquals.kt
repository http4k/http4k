package org.http4k.security

import java.security.MessageDigest

/**
 * Constant-time string comparison for secrets/tokens. Avoids leaking how many leading bytes
 * matched via timing. Note: differing lengths short-circuit, so input length is not protected.
 */
fun secureEquals(first: String?, second: String?): Boolean = when {
    first == null || second == null -> first == second
    else -> MessageDigest.isEqual(first.toByteArray(), second.toByteArray())
}
