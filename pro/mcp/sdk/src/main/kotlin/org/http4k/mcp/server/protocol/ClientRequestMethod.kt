package org.http4k.mcp.server.protocol

import org.http4k.mcp.model.ProgressToken

sealed interface ClientRequestMethod {
    val session: Session
    data class Stream(override val session: Session) : ClientRequestMethod
    data class RequestBased(val progressToken: ProgressToken, override val session: Session) : ClientRequestMethod
}
