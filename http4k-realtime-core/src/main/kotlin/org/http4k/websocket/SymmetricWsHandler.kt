package org.http4k.websocket

import org.http4k.core.Request
import org.http4k.core.Uri

typealias SymmetricWsHandler = (Request) -> Websocket

fun interface SymmetricWsFilter : (SymmetricWsHandler) -> SymmetricWsHandler {
    companion object
}

val SymmetricWsFilter.Companion.NoOp: SymmetricWsFilter get() = SymmetricWsFilter { next -> { next(it) } }
fun SymmetricWsFilter.then(next: SymmetricWsFilter): SymmetricWsFilter = SymmetricWsFilter { this(next(it)) }
fun SymmetricWsFilter.then(next: SymmetricWsHandler): SymmetricWsHandler = { this(next)(it) }

object SymmetricWsFilters {
    fun SetHostFrom(uri: Uri): SymmetricWsFilter = SymmetricWsFilter { next ->
        {
            next(it.uri(it.uri.scheme(uri.scheme).host(uri.host).port(uri.port))
                .replaceHeader("Host", "${uri.host}${uri.port?.let { port -> ":$port" } ?: ""}"))
        }
    }
}
