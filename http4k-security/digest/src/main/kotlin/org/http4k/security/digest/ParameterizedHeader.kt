package org.http4k.security.digest

data class ParameterizedHeader(
    val prefix: String,
    val parameters: Map<String, String?>) {

    fun toHeaderValue(quote: (String) -> Boolean): String {
        val paramString = parameters
            .mapValues { (key, value) -> if (quote(key)) "\"${value}\"" else value }
            .map { (key, value) -> "$key=$value" }
            .joinToString(", ")

        return "$prefix $paramString"
    }

    companion object {
        fun parse(headerValue: String): ParameterizedHeader {
            val (prefix, parameterList) = headerValue.split(" ", limit = 2)

            val parameters = parameterList
                .split(",")
                .filter { "=" in it }
                .associate {
                    val (key, value) = it.trim().split("=")
                    key.trim() to value.trim().replace("\"", "")
                }

            return ParameterizedHeader(prefix, parameters)
        }

        fun String.toParameterizedHeader() = parse(this)
    }
}
