/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.stateless.protocol.messages

import org.http4k.ai.mcp.stateless.model.ResultType

interface HasInputRequired {
    val resultType: ResultType
    val inputRequests: Map<String, InputRequest>?
    val requestState: String?
}
