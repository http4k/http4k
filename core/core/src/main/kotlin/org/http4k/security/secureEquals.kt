package org.http4k.security

import java.security.MessageDigest

/**
 * Constant-time string comparison for secrets/tokens. Avoids leaking how many leading bytes
 * matched via timing. Null inputs never match (including null vs null) to prevent fail-open
 * behaviour when a secret is absent. Both sides are hashed before comparison so input length
 * is not observable.
 */
fun secureEquals(first: String?, second: String?): Boolean = when {
    first == null || second == null -> false
    else -> MessageDigest.isEqual(Sha256.hash(first).toByteArray(), Sha256.hash(second).toByteArray())
}
