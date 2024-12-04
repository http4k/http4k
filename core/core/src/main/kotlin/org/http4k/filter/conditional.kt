package org.http4k.filter

import org.http4k.core.Filter
import org.http4k.core.Request
import org.http4k.core.then

fun Filter.thenIf(predicate: (Request) -> Boolean, filter: Filter) = then(Filter { next ->
    { request ->
        when {
            predicate(request) -> filter(next)(request)
            else -> next(request)
        }
    }
})

fun Filter.thenIfNot(predicate: (Request) -> Boolean, filter: Filter) = thenIf({ !predicate(it) }, filter)
