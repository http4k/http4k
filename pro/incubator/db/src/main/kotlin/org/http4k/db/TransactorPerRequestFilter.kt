package org.http4k.db

import org.http4k.core.Filter
import org.http4k.core.Request
import org.http4k.core.with
import org.http4k.lens.RequestKey

private const val TRANSACTION_RESOURCE_KEY = "http4k-db-transactor"

fun <Resource : Any> TransactionPerRequestFilter(transactor: Transactor<Resource>): Filter {
    val key = RequestKey.required<Resource>(TRANSACTION_RESOURCE_KEY)

    return Filter { next ->
        { request: Request ->
            transactor.perform { resource ->
                next(request.with(key of resource))
            }
        }
    }
}

fun <T : Any> Request.transactionResource(): T {
    val key = RequestKey.required<T>(TRANSACTION_RESOURCE_KEY)

    return key(this)
}
