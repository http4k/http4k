/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.server.protocol

import org.http4k.ai.a2a.protocol.messages.A2AMessage
import org.http4k.core.Request

interface Messages {
    fun send(params: A2AMessage.Send.Request.Params, http: Request): A2AMessage.Send.Response
    fun stream(params: A2AMessage.Stream.Request.Params, http: Request): Sequence<A2AMessage.Send.Response>
}
