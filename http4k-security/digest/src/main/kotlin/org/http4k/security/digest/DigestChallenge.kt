package org.http4k.security.digest

import org.http4k.appendIf
import org.http4k.appendIfPresent
import org.http4k.security.Nonce
import java.lang.StringBuilder

/**
 * The Digest Authorization challenge to be returned to the user as a header
 */
data class DigestChallenge(
    val realm: String,
    val nonce: Nonce,
    val algorithm: String?,
    val qop: List<Qop>,
    val opaque: String?
) {
    fun toHeaderValue(): String {
        val qopValue = qop.joinToString(", ") { it.value }

        return StringBuilder("Digest realm=\"$realm\", nonce=\"$nonce\"")
            .appendIfPresent(algorithm, ", algorithm=$algorithm")
            .appendIf({ qop.isNotEmpty()} , ", qop=\"$qopValue\"")
            .appendIfPresent(opaque, ", opaque=\"$opaque\"")
            .toString()
    }

    companion object {
        fun parse(headerValue: String): DigestChallenge? {
            val (prefix, parameters) = ParameterizedHeader.parse(headerValue)
            if (!prefix.startsWith("Digest")) return null

            return DigestChallenge(
                realm = parameters["realm"]!!,
                nonce = Nonce(parameters["nonce"]!!),
                algorithm = parameters["algorithm"],
                qop = (parameters["qop"] ?: "")
                    .split(",")
                    .mapNotNull { Qop.from(it.trim()) },
                opaque = parameters["opaque"]
            )
        }
    }
}
