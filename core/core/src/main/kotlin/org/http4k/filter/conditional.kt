package org.http4k.filter

import org.http4k.core.Filter
import org.http4k.core.Request
import org.http4k.core.then

typealias RequestPredicate = (Request) -> Boolean

fun Filter.thenIf(predicate: RequestPredicate, filter: Filter) = then(Filter { next ->
    { request ->
        when {
            predicate(request) -> filter(next)(request)
            else -> next(request)
        }
    }
})

fun Filter.thenIfNot(predicate: RequestPredicate, filter: Filter) = thenIf({ !predicate(it) }, filter)
