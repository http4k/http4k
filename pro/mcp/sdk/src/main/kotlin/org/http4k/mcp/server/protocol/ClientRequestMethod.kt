package org.http4k.mcp.server.protocol

import org.http4k.mcp.model.ProgressToken

sealed interface ClientRequestMethod {
    data class Stream(val session: Session) : ClientRequestMethod
    data class RequestBased(val progressToken: ProgressToken) : ClientRequestMethod
}
