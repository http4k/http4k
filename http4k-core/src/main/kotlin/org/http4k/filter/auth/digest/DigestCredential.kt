package org.http4k.filter.auth.digest

import org.http4k.appendIfPresent
import org.http4k.util.Hex

/**
 * The digest Authorization to pass to the server as a header
 */
data class DigestCredential(
    val realm: String,
    val username: String,
    val digestUri: String,
    val nonce: String,
    val response: String,
    val opaque: String?,
    val nonceCount: Long?,
    val algorithm: String?,
    val cnonce: String?,
    val qop: Qop?
) {
    fun toHeaderValue() = StringBuilder("Digest realm=\"$realm\", username=\"$username\", uri=\"$digestUri\", nonce=\"$nonce\", response=\"$response\"")
        .appendIfPresent(opaque, ", opaque=\"$opaque\"")
        .appendIfPresent(nonceCount, ", nc=${nonceCount?.toString(16)?.padStart(8, '0')}")
        .appendIfPresent(algorithm, ", algorithm=$algorithm")
        .appendIfPresent(cnonce, ", cnonce=\"$cnonce\"")
        .appendIfPresent(qop, ", qop=\"${qop?.value}\"")
        .toString()

    companion object {
        fun fromHeader(header: ParameterizedHeader): DigestCredential? {
            val (prefix, parameters) = header
            if (!prefix.equals("digest", ignoreCase = true)) return null

            return DigestCredential(
                realm = parameters["realm"] ?: return null,
                username = parameters["username"] ?: return null,
                digestUri = parameters["uri"] ?: return null,
                nonce = parameters["nonce"] ?: return null,
                opaque = parameters["opaque"],
                nonceCount = parameters["nc"]?.toLong(16),
                algorithm = parameters["algorithm"],
                response = parameters["response"] ?: return null,
                cnonce = parameters["cnonce"],
                qop = parameters["qop"]?.let { Qop.parse(it) }
            )
        }
    }

    fun responseBytes(): ByteArray? = try {
        Hex.unhex(response)
    } catch (e: NumberFormatException) {
        null
    }
}
