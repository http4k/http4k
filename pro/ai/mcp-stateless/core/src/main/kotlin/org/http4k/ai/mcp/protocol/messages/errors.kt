/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.protocol.messages

import org.http4k.jsonrpc.ErrorMessage

data class HeaderMismatchError(override val message: String) : ErrorMessage(CODE, message) {
    companion object {
        val CODE = -32001
    }
}

data class DomainError(override val message: String) : ErrorMessage(CODE, message) {
    companion object {
        val CODE = -32050
    }
}
