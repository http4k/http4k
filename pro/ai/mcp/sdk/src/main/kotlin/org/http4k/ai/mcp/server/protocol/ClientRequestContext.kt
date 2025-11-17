package org.http4k.ai.mcp.server.protocol

import org.http4k.ai.mcp.model.ProgressToken
import org.http4k.format.MoshiNode

/**
 * How a client is identified for sending a request to.
 */
sealed interface ClientRequestContext {
    val session: Session
    data class Subscription(override val session: Session) : ClientRequestContext
    data class ClientCall(val progressToken: ProgressToken, override val session: Session, val id: MoshiNode?) : ClientRequestContext
}
