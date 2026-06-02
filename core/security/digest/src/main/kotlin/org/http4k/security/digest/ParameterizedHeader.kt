package org.http4k.security.digest

data class ParameterizedHeader(
    val prefix: String,
    val parameters: Map<String, String?>
) {

    companion object {
        fun parse(headerValue: String): ParameterizedHeader {
            val parts = headerValue.trim().split(" ", limit = 2)
            val prefix = parts.getOrNull(0).orEmpty()
            val parameterList = parts.getOrNull(1).orEmpty()

            val parameters = parameterList
                .split(",")
                .filter { "=" in it }
                .associate {
                    val (key, value) = it.trim().split("=", limit = 2)
                    key.trim() to value.trim().replace("\"", "")
                }

            return ParameterizedHeader(prefix, parameters)
        }
    }
}
