package org.http4k.security

enum class ResponseMode(val queryParameterValue: String) {
    Query("query"),
    Fragment("fragment");
//    FormPost("form_post");

    companion object {
        fun fromQueryParameterValue(value: String): ResponseMode =
            ResponseMode.values().find { it.queryParameterValue == value }
                ?: throw IllegalArgumentException("Invalid response mode: $value")
    }
}
