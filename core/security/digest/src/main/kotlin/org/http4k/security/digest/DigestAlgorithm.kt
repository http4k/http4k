package org.http4k.security.digest

enum class DigestAlgorithm(val value: String) {
    MD5("MD5"),
    SHA_256("SHA-256");

    override fun toString() = value
}
