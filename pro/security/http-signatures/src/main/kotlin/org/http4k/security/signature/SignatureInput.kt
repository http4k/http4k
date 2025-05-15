package org.http4k.security.signature

import org.http4k.core.HttpMessage

/**
 * The makeup of a signature input in the HTTP Signature header.
 */
data class SignatureInput<M : HttpMessage>(
    val label: String,
    val components: List<SignatureComponent<M>>,
    val parameters: SignatureParameters
)

