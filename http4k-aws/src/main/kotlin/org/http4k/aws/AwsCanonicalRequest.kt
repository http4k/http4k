package org.http4k.aws

import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.core.toParameters
import org.http4k.urlEncoded

internal data class AwsCanonicalRequest(val value: String, val signedHeaders: String, val payloadHash: String) {
    companion object {
        fun of(request: Request): AwsCanonicalRequest {
            val signedHeaders = request.signedHeaders()
            val payloadHash = request.payloadHash()
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
                payloadHash
            return AwsCanonicalRequest(canonical, signedHeaders, payloadHash)
        }

        private fun Request.signedHeaders(): String =
            headers.map { it.first.toLowerCase() }.sorted().joinToString(";")

        private fun Request.canonicalHeaders(): String = headers
            .map { it.first.toLowerCase() to it.second?.replace("\\s+", " ")?.trim() }
            .map { it.first + ":" + it.second }
            .sorted()
            .joinToString("\n")

        private fun Request.canonicalQueryString(): String =
            uri.query.toParameters()
                .map { (first, second) -> first.urlEncoded() + "=" + second?.urlEncoded() }
                .sorted()
                .joinToString("&")

        private fun Request.payloadHash(): String = AwsHmacSha256.hash(body.payload.array())

        private fun Uri.normalisedPath() = path.split("/").map { it.urlEncoded() }.joinToString("/")
    }
}