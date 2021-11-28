package org.http4k.core

import java.net.URLDecoder
import java.net.URLEncoder
import java.util.regex.Pattern

data class UriTemplate private constructor(
    private val template: String,
    private val templateRegex: Regex,
    private val tokens: List<UriToken>,
) {
    companion object {
        fun from(template: String): UriTemplate {
            val trimmedTemplate = template.trim('/')
            val tokens = parseUriTemplate(trimmedTemplate)
            return UriTemplate(
                template = trimmedTemplate,
                templateRegex = tokens.toRegex(),
                tokens = tokens
            )
        }
    }

    fun matches(uri: String): Boolean = templateRegex.matches(uri.trim('/'))

    fun extract(uri: String): Map<String, String> {
        val match = templateRegex.find(uri.trim('/')) ?: return mapOf()
        return tokens.filterIsInstance<ParameterUriSegment>().associate {
            it.name to URLDecoder.decode(match.groups[it.name]!!.value)
        }
    }

    fun generate(parameters: Map<String, String>): String = tokens.toUri(parameters)

    override fun toString(): String = template
}

private sealed interface UriToken

private data class LiteralUriSegment(val text: String) : UriToken

private data class ParameterUriSegment(val name: String, val regex: String?) : UriToken

private fun List<UriToken>.toRegex(): Regex {
    val pattern = StringBuilder()
    for (token in this) {
        when (token) {
            is LiteralUriSegment -> pattern.append(Pattern.quote(token.text))
            is ParameterUriSegment -> {
                val name = token.name.trim()
                val groupName = if (name.isEmpty()) "" else "?<$name>"
                val regex = if (token.regex == null || name.isEmpty()) "[^/]+" else token.regex
                pattern.append("($groupName${regex})")
            }
        }
    }
    return Regex(pattern.toString())
}

private fun List<UriToken>.toUri(parameters: Map<String, String>): String {
    val uri = StringBuilder()
    for (token in this) {
        when (token) {
            is LiteralUriSegment -> uri.append(token.text)
            is ParameterUriSegment -> {
                val paramValue = parameters.getOrDefault(token.name, "")
                if (paramValue.contains("/")) {
                    uri.append(paramValue)
                } else {
                    uri.append(URLEncoder.encode(paramValue, "UTF-8"))
                }
            }
        }
    }
    return uri.toString()
}

private fun parseUriTemplate(template: String): List<UriToken> {
    val chars = template.iterator()
    val currentToken = StringBuilder()

    val tokens = ArrayList<UriToken>()
    while (chars.hasNext()) {
        var char = chars.next()
        if (char == '{') {
            // we found the beginning of a potential path parameter, so store
            // whatever token we've collected until now as a literal URI segment
            currentToken.addAsLiteralUriSegmentTo(tokens)

            // attempt to read the full contents of the path parameter
            var lastChar = char
            var openBraceCount = 1
            while (openBraceCount > 0 && chars.hasNext()) {
                char = chars.next()
                if (char == '}') {
                    // if parameter end is escaped we don't count it
                    if (lastChar != '\\')
                        openBraceCount--
                    if (openBraceCount > 0)
                    // we don't want to keep the very last time we encounter the ending
                    // character, but everything else we should keep
                        currentToken.append(char)
                } else if (char == '{') {
                    // if parameter start is escaped we don't count it
                    if (lastChar != '\\')
                        openBraceCount++
                    currentToken.append(char)
                } else {
                    currentToken.append(char)
                }
                lastChar = char
            }
            if (openBraceCount == 0) {
                // the parameter was successfully parsed, so add it as a parameter URI segment
                currentToken.addAsParameterUriSegmentTo(tokens)
            } else {
                // the parameter definition wasn't properly closed, consider it a literal
                currentToken.insert(0, '{')
                currentToken.addAsLiteralUriSegmentTo(tokens)
            }
        } else {
            currentToken.append(char)
        }
    }
    // Any remaining token is a literal URI segment
    currentToken.addAsLiteralUriSegmentTo(tokens)

    // We might have several consecutive segments that were identified as being literal URI segments,
    // so we collapse them all into one
    val reducedTokens = ArrayList<UriToken>()
    for (token in tokens) {
        if (token is LiteralUriSegment) {
            val lastIsLiteral = reducedTokens.isNotEmpty() && reducedTokens.last() is LiteralUriSegment
            if (lastIsLiteral) {
                val last = reducedTokens.removeLast() as LiteralUriSegment
                reducedTokens.add(LiteralUriSegment(text = last.text + token.text))
            } else {
                reducedTokens.add(token)
            }
        } else {
            reducedTokens.add(token)
        }
    }
    return reducedTokens
}

private fun StringBuilder.addAsLiteralUriSegmentTo(tokens: MutableList<UriToken>) {
    if (isEmpty()) return

    val segment = LiteralUriSegment(text = toString())
    tokens.add(segment)

    clear()
}

private fun StringBuilder.addAsParameterUriSegmentTo(tokens: MutableList<UriToken>) {
    if (isEmpty()) return

    val text = toString()
    val separatorIndex = text.indexOf(':')

    if (separatorIndex >= 0) {
        tokens.add(
            ParameterUriSegment(
                name = text.substring(0, separatorIndex),
                regex = text.substring(separatorIndex + 1)
            )
        )
    } else {
        tokens.add(ParameterUriSegment(name = text, regex = null))
    }

    clear()
}
