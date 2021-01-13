package org.http4k.core

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap


/**
 * In-memory RequestContext store. Override the storeId to use multiple stores in one app.
 */
class RequestContexts(storeId: String? = null) : Store<RequestContext> {
    private val requests = ConcurrentHashMap<UUID, RequestContext>()

    private val requestContextLens = RequestContext.lensForStore(storeId)

    override fun invoke(target: Request): RequestContext =
        requests[requestContextLens(target)] ?: throw IllegalStateException("No RequestContext initialised")

    override fun <R : Request> invoke(value: RequestContext, target: R): R {
        requests[value.id] = value
        return requestContextLens(value.id, target)
    }

    override fun remove(value: RequestContext): RequestContext? = requests.remove(value.id)
}
