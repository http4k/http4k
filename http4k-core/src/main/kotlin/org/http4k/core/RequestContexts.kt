package org.http4k.core

import org.http4k.lens.LensExtractor
import org.http4k.lens.LensFailure
import org.http4k.lens.LensInjector
import org.http4k.lens.Meta
import org.http4k.lens.Missing
import org.http4k.lens.ParamMeta
import java.util.*

class RequestContexts : LensInjector<Request, RequestContext>, LensExtractor<Request, RequestContext> {

    private val requests = mutableMapOf<UUID, RequestContext>()

    override fun invoke(target: Request): RequestContext =
        requests[RequestContext.extract(target)] ?: throw LensFailure(Missing(Meta(true, "context", ParamMeta.ObjectParam, "context")))

    override fun <R : Request> invoke(value: RequestContext, target: R): R {
        requests[value.id] = value
        return RequestContext.inject(value.id, target)
    }

    fun remove(requestContext: RequestContext): RequestContext? = requests.remove(requestContext.id)
}
