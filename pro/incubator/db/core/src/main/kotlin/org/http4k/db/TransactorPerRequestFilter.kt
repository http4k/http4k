package org.http4k.db

import org.http4k.core.Filter
import org.http4k.core.Request
import org.http4k.core.with
import org.http4k.lens.RequestKey

private const val TRANSACTION_RESOURCE_KEY = "http4k-db-transactor"

fun <Resource : Any> TransactionPerRequestFilter(
    transactor: Transactor<Resource>,
    keyName: String = TRANSACTION_RESOURCE_KEY
): Filter {
    val key = RequestKey.required<Resource>(keyName)

    return Filter { next ->
        { request: Request ->
            transactor.perform { resource ->
                next(request.with(key of resource))
            }
        }
    }
}

fun <T : Any> Request.transactionResource(keyName: String = TRANSACTION_RESOURCE_KEY): T {
    val key = RequestKey.required<T>(keyName)

    return key(this)
}
