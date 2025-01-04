package org.http4k.filter

import dev.failsafe.BulkheadFullException
import dev.failsafe.CircuitBreakerOpenException
import dev.failsafe.FailsafeException
import dev.failsafe.FailsafeExecutor
import dev.failsafe.RateLimitExceededException
import dev.failsafe.TimeoutExceededException
import dev.failsafe.function.CheckedSupplier
import org.http4k.core.Filter
import org.http4k.core.Response
import org.http4k.core.Status

object FailsafeFilter {

    operator fun invoke(
        failsafeExecutor: FailsafeExecutor<Response>,
        onError: (FailsafeException) -> Response = defaultErrorHandler
    ) = Filter { next ->
        { request ->
            try {
                failsafeExecutor.get(CheckedSupplier { next(request) })
            } catch (e: FailsafeException) {
                onError(e)
            }
        }
    }

    private val defaultErrorHandler: (FailsafeException) -> Response = {
        when (it) {
            is CircuitBreakerOpenException ->
                Response(Status.SERVICE_UNAVAILABLE.description("Circuit is open"))
            is BulkheadFullException ->
                Response(Status.TOO_MANY_REQUESTS.description("Bulkhead limit exceeded"))
            is TimeoutExceededException ->
                Response(Status.CLIENT_TIMEOUT)
            is RateLimitExceededException ->
                Response(Status.TOO_MANY_REQUESTS.description("Rate limit exceeded"))
            else -> throw it
        }
    }
}
