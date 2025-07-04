package org.http4k.security

enum class ResponseType(val queryParameterValue: String) {
    Code("code"),
    Token("token"),
    CodeIdToken("code id_token"),
    CodeToken("code token");

    companion object {
        fun fromQueryParameterValue(value: String): ResponseType =
            entries.find { it.queryParameterValue == value }
                ?: throw IllegalArgumentException("Invalid response type: $value")
    }
}
