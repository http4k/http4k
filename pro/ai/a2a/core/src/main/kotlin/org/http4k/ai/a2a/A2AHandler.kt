/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a

import org.http4k.ai.a2a.protocol.messages.A2AJsonRpcRequest
import org.http4k.ai.a2a.protocol.messages.A2AJsonRpcResponse
import org.http4k.core.Request

data class A2ARequest(val message: A2AJsonRpcRequest, val http: Request)

sealed interface A2AResponse {
    data class Single(val message: A2AJsonRpcResponse) : A2AResponse
    data class Stream(val messages: Sequence<A2AJsonRpcResponse>) : A2AResponse
}

typealias A2AHandler = (A2ARequest) -> A2AResponse
