package org.http4k.contract.security

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.core.with
import org.http4k.lens.Lens
import org.http4k.lens.LensFailure
import org.http4k.lens.LensInjector

/**
 * Checks the presence of the named Api Key parameter. Filter returns 401 if Api-Key is not found in request.
 *
 * Default implementation of ApiKey. Includes an option to NOT authorise OPTIONS requests, which is
 * currently not enabled for OpenAPI.
 */
sealed interface ApiKeySecurity<out T> : Security {
    val param: Lens<Request, T>
    val name: String

    companion object {
        operator fun <T> invoke(
            param: Lens<Request, T>,
            validateKey: (T) -> Boolean,
            authorizeOptionsRequests: Boolean = true,
            name: String = "api_key"
        ): ApiKeySecurity<T> = SimpleApiKeySecurity(param, validateKey, authorizeOptionsRequests, name)

        operator fun <T, C> invoke(
            param: Lens<Request, T>,
            consumer: LensInjector<C, Request>,
            retrieveConsumerForKey: (T) -> C?,
            authorizeOptionsRequests: Boolean = true,
            name: String = "api_key"
        ): ApiKeySecurity<T> = ApiKeySecurityWithConsumer(param, consumer, retrieveConsumerForKey, authorizeOptionsRequests, name)
    }
}

private fun <T, R> apiKeySecurityFilter(
    param: Lens<Request, T>,
    validateAndReturnResult: (T) -> R,
    failureResult: R,
    authorizeOptionsRequests: Boolean = true,
    onSuccess: (next: HttpHandler, request: Request, R) -> Response?
) = Filter { next ->
    {
        if (!authorizeOptionsRequests && it.method == Method.OPTIONS) {
            next(it)
        } else {
            val keyValid = try {
                validateAndReturnResult(param(it))
            } catch (e: LensFailure) {
                failureResult
            }
            onSuccess(next, it, keyValid) ?: Response(UNAUTHORIZED)
        }
    }
}

internal class SimpleApiKeySecurity<out T>(
    override val param: Lens<Request, T>,
    validateKey: (T) -> Boolean,
    authorizeOptionsRequests: Boolean = true,
    override val name: String
) : ApiKeySecurity<T> {
    override val filter =
        apiKeySecurityFilter(param, validateKey, failureResult = false, authorizeOptionsRequests) { next, request, result ->
            if(result) next(request) else null
        }
}

internal class ApiKeySecurityWithConsumer<out T, in C>(
    override val param: Lens<Request, T>,
    private val consumer: LensInjector<C, Request>,
    retrieveConsumerForKey: (T) -> C?,
    authorizeOptionsRequests: Boolean = true,
    override val name: String
) : ApiKeySecurity<T> {
    override val filter =
        apiKeySecurityFilter(param, retrieveConsumerForKey, failureResult = null, authorizeOptionsRequests) { next, request, result ->
            if(result != null) next(request.with(consumer of result)) else null
        }
}
