package org.http4k.filter

import org.http4k.core.RequestContext
import org.http4k.core.Store
import org.http4k.sse.SseFilter
import org.http4k.websocket.WsFilter

fun ServerFilters.InitialiseSseRequestContext(contexts: Store<RequestContext>) = SseFilter { next ->
    {
        val context = RequestContext()
        try {
            next(contexts(context, it))
        } finally {
            contexts.remove(context)
        }
    }
}

fun ServerFilters.InitialiseWsRequestContext(contexts: Store<RequestContext>) = WsFilter { next ->
    {
        val context = RequestContext()
        try {
            next(contexts(context, it))
        } finally {
            contexts.remove(context)
        }
    }
}

fun ServerFilters.SetWsSubProtocol(subprotocol: String) = WsFilter { next ->
    {
        next(it).copy(subprotocol = subprotocol)
    }
}
