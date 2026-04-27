/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.protocol.messages

import org.http4k.ai.a2a.util.A2ANodeType
import org.http4k.jsonrpc.ErrorMessage
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class A2AJsonRpcErrorResponse(
    override val id: Any?,
    val error: A2ANodeType,
    val jsonrpc: String = "2.0"
) : A2AJsonRpcResponse() {
    constructor(id: Any?, error: ErrorMessage) : this(id, error(org.http4k.ai.a2a.util.A2AJson))
}
