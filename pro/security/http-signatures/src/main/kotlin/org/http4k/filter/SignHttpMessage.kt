package org.http4k.filter

import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.onFailure
import org.http4k.core.Filter
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.security.signature.HttpMessageSigner

/**
 * Add a RFC-9421 HTTP signature to requests
 */
fun ClientFilters.SignHttpRequest(signer: HttpMessageSigner<Request, *, *>) =
    Filter { next ->
        { req ->
            signer(req, req)
                .map(next)
                .onFailure { throw IllegalStateException("Failed to create signature: $it") }
        }
    }

/**
 * Add a RFC-9421 HTTP signature to responses
 */
fun ServerFilters.SignHttpResponse(signer: HttpMessageSigner<Response, *, *>) =
    Filter { next ->
        { req ->
            signer(req, next(req))
                .onFailure { throw IllegalStateException("Failed to create signature: $it") }
        }
    }
