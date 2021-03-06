package org.http4k.filter

import org.http4k.core.Filter
import org.http4k.core.Request

typealias RequestPredicate = (Request) -> Boolean

class PATH_STARTS_WITH(val urlPrefix: String) : RequestPredicate {
    override fun invoke(request: Request): Boolean {
        return request.uri.path.startsWith(urlPrefix)
    }
}

fun Filter.thenIf(predicate: RequestPredicate, filter: Filter): Filter {
    return Filter { next ->
        { request ->
            if (predicate(request))
                filter(next)(request)
            else
                next(request)
        }
    }
}

fun Filter.thenIfNot(predicate: RequestPredicate, filter: Filter): Filter {
    return thenIf({ !predicate(it) }, filter)
}
