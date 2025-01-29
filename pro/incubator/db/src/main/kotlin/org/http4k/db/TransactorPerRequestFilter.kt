package org.http4k.db

import org.http4k.core.Filter
import org.http4k.core.NoOp
import org.http4k.core.Request
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.lens.BiDiLens
import org.http4k.lens.RequestContextKey
import org.http4k.lens.RequestKey


fun <T : Any> TransactionPerRequestFilter(transactor: Transactor<T>) =
    Filter.NoOp
        .then(Filter { next ->
            { request: Request ->
                transactor.perform { resource ->
                    next(Transactor.withTransactionResource(request, resource))
                }
            }
        })


