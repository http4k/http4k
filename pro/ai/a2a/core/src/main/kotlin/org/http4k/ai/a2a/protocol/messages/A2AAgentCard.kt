/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.protocol.messages

import org.http4k.ai.a2a.model.AgentCard
import org.http4k.ai.a2a.protocol.A2ARpcMethod.Companion.of
import se.ansman.kotshi.JsonSerializable
import se.ansman.kotshi.PolymorphicLabel

object A2AAgentCard {
    object GetExtended {
        @JsonSerializable
        @PolymorphicLabel("getExtendedAgentCard")
        data class Request(
            override val id: Any?,
            val jsonrpc: String = "2.0"
        ) : A2AJsonRpcRequest() {
            override val method = of("getExtendedAgentCard")
        }

        @JsonSerializable
        data class Response(
            val result: AgentCard,
            override val id: Any?,
            val jsonrpc: String = "2.0"
        ) : A2AJsonRpcResponse
    }
}
