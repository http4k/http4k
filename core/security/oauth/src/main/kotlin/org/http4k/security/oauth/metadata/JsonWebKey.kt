package org.http4k.security.oauth.metadata

data class JsonWebKey(
    val kty: String,
    val use: String? = null,
    val kid: String? = null,
    val alg: String? = null,
    val n: String? = null,
    val e: String? = null,
    val x5c: List<String>? = null,
    val x5t: String? = null,
    val `x5t#S256`: String? = null
)
