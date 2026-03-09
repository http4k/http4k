/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.mcp

import org.http4k.ai.mcp.protocol.messages.McpPrompt

data class McpFieldView(
    val name: String,
    val description: String,
    val required: Boolean,
    val type: String,
    val enumValues: List<String>,
    val defaultValue: String
) {
    val isBoolean get() = type == "boolean"
    val isEnum get() = enumValues.isNotEmpty()
    val isText get() = !isBoolean && !isEnum
}

@Suppress("UNCHECKED_CAST")
fun Map<String, Any>.toFieldViews(): List<McpFieldView> {
    val properties = this["properties"] as? Map<String, Map<String, Any>> ?: return emptyList()
    val requiredFields = (this["required"] as? List<String>) ?: emptyList()

    return properties.entries
        .map { (name, prop) ->
            McpFieldView(
                name = name,
                description = (prop["description"] as? String) ?: "",
                required = name in requiredFields,
                type = (prop["type"] as? String) ?: "string",
                enumValues = (prop["enum"] as? List<String>) ?: emptyList(),
                defaultValue = prop["default"]?.toString() ?: ""
            )
        }
        .sortedBy { it.name }
}

fun McpPrompt.Argument.toFieldView() = McpFieldView(
    name = name,
    description = description ?: "",
    required = required ?: false,
    type = "string",
    enumValues = emptyList(),
    defaultValue = ""
)
