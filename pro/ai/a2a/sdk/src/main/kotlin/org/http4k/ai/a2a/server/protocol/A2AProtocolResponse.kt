package org.http4k.ai.a2a.server.protocol

import org.http4k.ai.a2a.util.A2ANodeType

sealed interface A2AProtocolResponse {
    data class Single(val response: A2ANodeType) : A2AProtocolResponse
    data class Stream(val responses: Sequence<A2ANodeType>) : A2AProtocolResponse
}
