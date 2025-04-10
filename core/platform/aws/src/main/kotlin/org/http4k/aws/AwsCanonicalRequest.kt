package org.http4k.aws

import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.core.toParameters
import org.http4k.filter.CanonicalPayload
import org.http4k.urlEncoded
import java.util.Locale.getDefault

internal data class AwsCanonicalRequest(val value: String, val signedHeaders: String, val payloadHash: String) {
    companion object {
        fun of(request: Request, payload: CanonicalPayload): AwsCanonicalRequest {
            val signedHeaders = request.signedHeaders()
            val canonical = request.method.name +
                "\n" +
                request.uri.normalisedPath() +
                "\n" +
                request.canonicalQueryString() +
                "\n" +
                request.canonicalHeaders() +
                "\n\n" +
                signedHeaders +
                "\n" +
                payload.hash
            return AwsCanonicalRequest(canonical, signedHeaders, payload.hash)
        }

        private val multipleSpaces = Regex("\\s+")
        private fun Request.canonicalHeaders(): String = headers
            .map { it.first.lowercase(getDefault()) to (it.second?.replace(multipleSpaces, " ")?.trim().orEmpty()) }
            .groupBy({ it.first }) { it.second }
            .mapValues { it.value.joinToString(",") }
            .toList()
            .sortedBy { it.first }
            .joinToString("\n") { it.first + ":" + it.second }

        private fun Request.canonicalQueryString(): String =
            uri.query.toParameters()
                .map { (first, second) -> first.urlEncoded() + "=" + second?.urlEncoded().orEmpty() }
                .sorted()
                .joinToString("&")

        private fun Uri.normalisedPath() =
            path.let { if (it.startsWith("/")) it else "/$it" }
    }
}


internal fun Request.signedHeaders(): String =
    headers.map { it.first.lowercase(getDefault()) }.toSet().sorted().joinToString(";")

