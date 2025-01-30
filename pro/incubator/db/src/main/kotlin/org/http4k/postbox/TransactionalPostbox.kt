package org.http4k.postbox

import dev.forkhandles.result4k.get
import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.mapFailure
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.ACCEPTED
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.db.Transactor
import org.http4k.db.performAsResult

fun TransactionalPostbox(transactor: Transactor<Postbox>): HttpHandler {
    return { req: Request ->
        transactor.performAsResult { it.store(req) }
            .map { Response(ACCEPTED) }
            .mapFailure { Response(INTERNAL_SERVER_ERROR.description(it.message ?: "")) }
            .get()
    }
}

interface Postbox {
    fun store(request: Request)
}
