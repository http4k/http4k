package org.http4k.security.digest

data class ParameterizedHeader(
    val prefix: String,
    val parameters: Map<String, String?>) {

    companion object {
        fun parse(headerValue: String): ParameterizedHeader {
            val (prefix, parameterList) = headerValue.trim().split(" ", limit = 2)

            val parameters = parameterList
                .split(",")
                .filter { "=" in it }
                .associate {
                    val (key, value) = it.trim().split("=")
                    key.trim() to value.trim().replace("\"", "")
                }

            return ParameterizedHeader(prefix, parameters)
        }
    }
}
