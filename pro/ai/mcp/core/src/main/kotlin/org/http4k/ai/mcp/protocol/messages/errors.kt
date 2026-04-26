/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.protocol.messages

import org.http4k.ai.mcp.util.McpJson
import org.http4k.format.Json
import org.http4k.jsonrpc.ErrorMessage
import se.ansman.kotshi.JsonSerializable

data class DomainError(override val message: String) : ErrorMessage(CODE, message) {
    companion object {
        val CODE = -32050
    }
}

@JsonSerializable
data class URLElicitationRequiredError(
    val elicitations: List<McpElicitations.Request.Url>,
    override val message: String
) : ErrorMessage(CODE, message) {

    override fun toString() = "URLElicitationRequiredError(elicitations=$elicitations, message='$message')"

    override fun <NODE> data(json: Json<NODE>): NODE = json {
        obj("elicitations" to array(elicitations.map { json.parse(McpJson.asFormatString(it)) }))
    }

    companion object {
        val CODE = -32042
    }
}
