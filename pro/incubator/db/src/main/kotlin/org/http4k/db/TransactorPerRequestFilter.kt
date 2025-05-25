package org.http4k.db

import kotlinx.coroutines.runBlocking
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
                runBlocking {
                    next(request.with(key of resource)) // FIXME coroutine blocking
                }
            }
        }
    }
}

fun <T : Any> Request.transactionResource(): T {
    val key = RequestKey.required<T>(TRANSACTION_RESOURCE_KEY)

    return key(this)
}
