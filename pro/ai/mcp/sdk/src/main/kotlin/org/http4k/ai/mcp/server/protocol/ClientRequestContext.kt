/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.protocol

/**
 * How a client is identified for sending a request to.
 */
sealed interface ClientRequestContext {
    val session: Session
    data class Subscription(override val session: Session) : ClientRequestContext
    data class ClientCall(override val session: Session) : ClientRequestContext
}
