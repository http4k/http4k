package org.http4k.metrics

import org.http4k.filter.HttpTransactionLabeler

data class MetricsDefaults(
    val counterDescription: Pair<String, String>,
    val timerDescription: Pair<String, String>,
    val labeler: HttpTransactionLabeler
) {
    companion object {
        private val notAlphaNumUnderscore = "[^a-zA-Z0-9_]".toRegex()
        private val urlTemplates = "\\{(.+):.+}".toRegex()

        private fun String.replaceRegexes() = replace(urlTemplates, "$1")

        val server = MetricsDefaults(
            "http.server.request.count" to "Total number of server requests",
            "http.server.request.latency" to "Timing of server requests"
        ) {
            it.copy(
                labels = mapOf(
                    "method" to it.request.method.toString(),
                    "status" to it.response.status.code.toString(),
                    "path" to it.routingGroup.replace('/', '_').replaceRegexes().replace('.', '_').replace(notAlphaNumUnderscore, "")
                )
            )
        }

        val client = MetricsDefaults(
            "http.client.request.count" to "Total number of client requests",
            "http.client.request.latency" to "Timing of client requests"
        ) {
            it.copy(
                labels = mapOf(
                    "method" to it.request.method.toString(),
                    "status" to it.response.status.code.toString(),
                    "host" to it.request.uri.host.replace('.', '_'),
                    "path" to it.routingGroup.replace('/', '_').replaceRegexes().replace('.', '_').replace(notAlphaNumUnderscore, "")
                )
            )
        }
    }
}

