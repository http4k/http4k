package org.http4k.websocket

import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.security.Security
import org.http4k.websocket.WsStatus.Companion.REFUSE

fun WsFilter(security: Security) = WsFilter { next ->
    {
        var targetRequest = it
        val authResponse = security.filter.then {
            targetRequest = it
            Response(OK)
        }(it)
        when {
            authResponse.status.successful -> next(targetRequest)
            else -> WsResponse { it.close(REFUSE) }
        }
    }
}
