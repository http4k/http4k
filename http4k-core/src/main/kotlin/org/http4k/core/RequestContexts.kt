package org.http4k.core

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory RequestContext store.
 */
class RequestContexts : Store<RequestContext> {

    private val requests = ConcurrentHashMap<UUID, RequestContext>()

    override fun invoke(target: Request): RequestContext =
        requests[RequestContext(target)] ?: throw IllegalStateException("No RequestContext initialised")

    override fun <R : Request> invoke(value: RequestContext, target: R): R {
        requests[value.id] = value
        return RequestContext(value.id, target)
    }

    override fun remove(value: RequestContext): RequestContext? = requests.remove(value.id)
}
