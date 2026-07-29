/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.stateless.util

import org.http4k.ai.mcp.stateless.model.McpUri
import org.http4k.ai.mcp.stateless.model.ResourceUriTemplate
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Implementation of RFC 6570 URI Templates (Level 1 and Level 2)
 *
 * This implementation supports:
 * - Level 1: Simple string expansion {var}
 * - Level 2: Reserved expansion {+var} and Fragment expansion {#var}
 *
 * It provides both template matching and expansion functionality.
 */
internal object Rfc6570UriTemplateMatcher {

    private val templateExpressionRegex = "\\{([+#]?)([^}]+)}".toRegex()
    private val patterns = ConcurrentHashMap<String, Regex>()

    /**
     * Checks if a URI matches a template
     *
     * @param testUri The URI to check
     * @return true if the URI matches the template, false otherwise
     */
    fun ResourceUriTemplate.matches(mcpUri: McpUri): Boolean {
        val template = value
        val uri = mcpUri.value

        if (uri == template) return true

        return try {
            patterns.computeIfAbsent(template, ::convertTemplateToPattern).matches(uri)
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Expands a URI template with the given variable values
     *
     * @param template The URI template to expand
     * @param variables Map of variable names to their values
     * @return The expanded URI
     */
    fun expand(template: String, variables: Map<String, Any>): String {
        val result = StringBuilder()
        var pos = 0

        for (match in templateExpressionRegex.findAll(template)) {
            result.append(template.substring(pos, match.range.first))

            val operator = match.groupValues[1]
            val variableList = match.groupValues[2]

            when (operator) {
                "+" -> result.append(expandReserved(variableList, variables))

                "#" -> {
                    val expanded = expandReserved(variableList, variables)
                    if (expanded.isNotEmpty()) {
                        result.append("#").append(expanded)
                    }
                }

                else -> result.append(expandSimple(variableList, variables))
            }

            pos = match.range.last + 1
        }

        if (pos < template.length) {
            result.append(template.substring(pos))
        }

        return result.toString()
    }

    /**
     * Converts a template to a regex pattern for matching
     */
    private fun convertTemplateToPattern(template: String): Regex {
        val patternBuilder = StringBuilder("^")
        var pos = 0

        for (match in templateExpressionRegex.findAll(template)) {
            val literal = template.substring(pos, match.range.first)
            patternBuilder.append(escapeRegex(literal))

            val operator = match.groupValues[1]
            val variableSpec = match.groupValues[2]

            val variablePattern = when (operator) {
                // Level 2: Reserved expansion
                "+" -> {
                    if (variableSpec == "path") {
                        "([^/]+(?:/[^/]+)*|/[^/]*(?:/[^/]*)*|)"
                    } else {
                        "([^/]*)"
                    }
                }

                // Level 2: Fragment expansion
                "#" -> "(.*)"

                // Level 1: Simple string expansion - also handle empty values
                else -> {
                    if (variableSpec == "empty") {
                        "()"
                    } else {
                        "([^/]*)"
                    }
                }
            }

            patternBuilder.append(variablePattern)
            pos = match.range.last + 1
        }

        if (pos < template.length) {
            patternBuilder.append(escapeRegex(template.substring(pos)))
        }

        patternBuilder.append("$")
        return patternBuilder.toString().toRegex(RegexOption.IGNORE_CASE)
    }

    /**
     * Expands a simple string expression (Level 1)
     */
    private fun expandSimple(variableList: String, variables: Map<String, Any>): String {
        val varSpecs = variableList.split(",")
        val expanded = varSpecs.mapNotNull { varSpec ->
            val varName = varSpec.trim()
            val value = variables[varName]

            if (value != null) {
                if (value is String && value.isEmpty()) {
                    ""
                } else {
                    percentEncode(value.toString())
                }
            } else {
                null
            }
        }

        return expanded.joinToString(",")
    }

    /**
     * Expands a reserved expression (Level 2)
     */
    private fun expandReserved(variableList: String, variables: Map<String, Any>): String {
        val varSpecs = variableList.split(",")
        val expanded = varSpecs.mapNotNull { varSpec ->
            val varName = varSpec.trim()
            val value = variables[varName]

            if (value != null) {
                if (value is String && value.isEmpty()) {
                    ""
                } else {
                    percentEncodeReserved(value.toString())
                }
            } else {
                null
            }
        }

        return expanded.joinToString(",")
    }

    /**
     * Percent-encodes a string for normal template expansion
     * Encodes all characters except unreserved (RFC 3986)
     */
    private fun percentEncode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8)
        .replace("+", "%20") // Replace space encoding from + to %20
        .replace("%7E", "~") // Don't encode tilde as it's unreserved

    /**
     * Percent-encodes a string for reserved expansion, preserving reserved characters
     * Only encodes characters that are not in unreserved or reserved set (RFC 3986)
     */
    private fun percentEncodeReserved(value: String): String {
        val result = StringBuilder()

        for (char in value) {
            when {
                char.isUnreservedChar() -> result.append(char)

                char.isReservedChar() -> result.append(char)

                else -> {
                    char.toString().toByteArray(StandardCharsets.UTF_8).forEach { byte ->
                        result.append(String.format(Locale.ROOT, "%%%02X", byte))
                    }
                }
            }
        }

        return result.toString()
    }

    /**
     * Checks if a character is in the unreserved set (RFC 3986)
     */
    private fun Char.isUnreservedChar(): Boolean =
        isLetterOrDigit() || this == '-' || this == '.' || this == '_' || this == '~'

    /**
     * Checks if a character is in the reserved set (RFC 3986)
     */
    private fun Char.isReservedChar(): Boolean = this in "/:?#[]@!$&'()*+,;="

    /**
     * Escapes special regex characters for pattern matching
     */
    private fun escapeRegex(input: String): String {
        val specialChars = ".^$*+?()[]{}|\\/"
        return input.map { char ->
            if (char in specialChars) "\\$char" else char.toString()
        }.joinToString("")
    }
}
