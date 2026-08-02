/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.protocol.messages

import org.http4k.ai.mcp.protocol.ProtocolVersion
import org.http4k.core.Uri
import org.http4k.format.Json
import org.http4k.jsonrpc.ErrorMessage

// MCP-reserved error codes (2026-07-28) live in -32020..-32099; -32000..-32019 stays implementation-defined.
data class HeaderMismatchError(override val message: String) : ErrorMessage(CODE, message) {
    companion object {
        val CODE = -32020
    }
}

data class MissingRequiredClientCapabilityError(
    val requiredCapabilities: List<String>
) : ErrorMessage(CODE, "Missing required client capability") {
    override fun <NODE> data(json: Json<NODE>): NODE = json {
        obj("requiredCapabilities" to obj(requiredCapabilities.map { it to obj() }))
    }

    companion object {
        val CODE = -32021
    }
}

data class UnsupportedProtocolVersionError(
    val requested: ProtocolVersion,
    val supported: List<ProtocolVersion>
) : ErrorMessage(CODE, "Unsupported protocol version") {
    override fun <NODE> data(json: Json<NODE>): NODE = json {
        obj(
            "requested" to string(requested.value),
            "supported" to array(supported.map { string(it.value) })
        )
    }

    companion object {
        val CODE = -32022
    }
}

data class DomainError(override val message: String) : ErrorMessage(CODE, message) {
    companion object {
        val CODE = -32050
    }
}

// SEP-2164: resource-not-found is InvalidParams (-32602) carrying the requested uri in `data`.
data class ResourceNotFoundError(val uri: Uri) : ErrorMessage(InvalidParams.code, "Resource not found") {
    override fun <NODE> data(json: Json<NODE>): NODE = json {
        obj("uri" to string(uri.toString()))
    }
}
