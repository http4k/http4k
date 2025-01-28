package org.http4k.db

import org.http4k.core.Filter
import org.http4k.core.NoOp
import org.http4k.core.Request
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.lens.BiDiLens
import org.http4k.lens.RequestContextKey
import org.http4k.lens.RequestKey

private const val KEY = "http4k-db-transactor"

fun <T : Any> TransactionPerRequestFilter(transactor: Transactor<T>) =
    Filter.NoOp
        .then(Filter { next ->
            { request: Request ->
                val lens: BiDiLens<Request, T> = transactionResourceLens()
                transactor.perform { resource ->
                    next(request.with(lens of resource))
                }
            }
        })

fun <T : Any> transactionResourceLens(): BiDiLens<Request, T> = RequestKey.of(KEY)

fun <Resource : Any> Request.transactionResource() = transactionResourceLens<Resource>()(this)

