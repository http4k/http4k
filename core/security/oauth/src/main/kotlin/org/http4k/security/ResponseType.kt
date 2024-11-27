package org.http4k.security

enum class ResponseType(val queryParameterValue: String) {
    Code("code"),
    CodeIdToken("code id_token");

    companion object {
        fun fromQueryParameterValue(value: String): ResponseType =
            entries.find { it.queryParameterValue == value }
                ?: throw IllegalArgumentException("Invalid response type: $value")
    }
}
