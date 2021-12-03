package org.http4k.core

import java.net.URLDecoder
import java.net.URLEncoder
import java.util.regex.Pattern

data class UriTemplate private constructor(private val template: String) {
    private val templateRegex = template.replace(URI_TEMPLATE_FORMAT,
        { notMatched -> Pattern.quote(notMatched) },
        { matched ->
            when {
                matched.groupValues[2].isBlank() -> "([^/]+)"
                else -> "(${matched.groupValues[2]})"
            }
        }).toRegex()

    private val matches = URI_TEMPLATE_FORMAT.findAll(template)
    private val parameterNames = matches.map { it.groupValues[1] }.toList()

    companion object {
        private val URI_TEMPLATE_FORMAT = "\\{([^}]+?)(?::([^}]+))?\\}".toRegex() // ignore redundant warning #100
        fun from(template: String) = UriTemplate(template.trimSlashes())

        private fun String.trimSlashes() = "^(/)?(.*?)(/)?$".toRegex().replace(this) { result -> result.groupValues[2] }
    }

    fun matches(uri: String): Boolean = templateRegex.matches(uri.trimSlashes())

    fun extract(uri: String): Map<String, String> =
        parameterNames.zip(templateRegex.findParameterValues(uri.trimSlashes())).toMap()

    fun generate(parameters: Map<String, String>): String =
        template.replace(URI_TEMPLATE_FORMAT) { matchResult ->
            val paramValue = parameters[matchResult.groupValues[1]] ?: ""
            if (paramValue.contains("/")) paramValue else URLEncoder.encode(paramValue, "UTF-8")
        }

    private fun Regex.findParameterValues(uri: String): List<String> =
        findAll(uri).first().groupValues.drop(1).map { URLDecoder.decode(it, "UTF-8") }

    private fun String.replace(regex: Regex, notMatched: (String) -> String, matched: (MatchResult) -> String): String {
        val matches = regex.findAll(this)
        val builder = StringBuilder()
        var position = 0
        for (matchResult in matches) {
            val before = substring(position, matchResult.range.first)
            if (before.isNotEmpty()) builder.append(notMatched(before))
            builder.append(matched(matchResult))
            position = matchResult.range.last + 1
        }
        val after = substring(position, length)
        if (after.isNotEmpty()) builder.append(notMatched(after))
        return builder.toString()
    }

    override fun toString(): String = template
}
