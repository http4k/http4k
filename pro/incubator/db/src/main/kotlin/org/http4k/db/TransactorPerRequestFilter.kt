package org.http4k.db

import org.http4k.core.Filter
import org.http4k.core.NoOp
import org.http4k.core.Request
import org.http4k.core.RequestContexts
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.ServerFilters.InitialiseRequestContext
import org.http4k.lens.BiDiLens
import org.http4k.lens.RequestContextKey

private const val KEY = "http4k-db-transactor"

fun <T> TransactionPerRequestFilter(contexts: RequestContexts, transactor: Transactor<T>) =
    Filter.NoOp
        .then(InitialiseRequestContext(contexts))
        .then(Filter { next ->
            { request: Request ->
                val lens: BiDiLens<Request, T> = transactionResourceLens(contexts)
                transactor.perform { resource ->
                    next(request.with(lens of resource))
                }
            }
        })

fun <T> transactionResourceLens(contexts: RequestContexts) = RequestContextKey.required<T>(contexts, KEY)

fun <Resource> Request.transactionResource(contexts: RequestContexts) =
    transactionResourceLens<Resource>(contexts)(this)

