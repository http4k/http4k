package org.http4k.core

import org.http4k.lens.LensExtractor
import org.http4k.lens.LensInjector
import java.util.*

class RequestContexts : LensInjector<RequestContext, Request>, LensExtractor<Request, RequestContext> {

    private val requests = mutableMapOf<UUID, RequestContext>()

    override fun invoke(target: Request): RequestContext =
        requests[RequestContext.extract(target)] ?: throw IllegalStateException("No RequestContext initialised")

    override fun <R : Request> invoke(value: RequestContext, target: R): R {
        requests[value.id] = value
        return RequestContext.inject(value.id, target)
    }

    fun remove(requestContext: RequestContext): RequestContext? = requests.remove(requestContext.id)
}
