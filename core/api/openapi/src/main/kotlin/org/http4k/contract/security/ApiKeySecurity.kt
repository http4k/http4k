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
class ApiKeySecurity<out T> private constructor(
    val param: Lens<Request, T>,
    val name: String,
    override val filter: Filter
) : Security {

    constructor(
        param: Lens<Request, T>,
        validateKey: (T) -> Boolean,
        authorizeOptionsRequests: Boolean = true,
        name: String = "api_key"
    ) : this(param, name, apiKeySecurityFilter(
        param,
        validateKey,
        failureResult = false,
        authorizeOptionsRequests
    ) { next, request, result -> if (result) next(request) else null }
    )

    companion object {
        operator fun <T, C> invoke(
            param: Lens<Request, T>,
            consumer: LensInjector<C, Request>,
            consumerForKey: (T) -> C?,
            authorizeOptionsRequests: Boolean = true,
            name: String = "api_key"
        ): ApiKeySecurity<T> = ApiKeySecurity(param, name,
            apiKeySecurityFilter(
                param,
                consumerForKey,
                failureResult = null,
                authorizeOptionsRequests
            ) { next, request, result ->
                if (result != null) next(request.with(consumer of result)) else null
            }
        )

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
                    val result = try {
                        validateAndReturnResult(param(it))
                    } catch (e: LensFailure) {
                        failureResult
                    }
                    onSuccess(next, it, result) ?: Response(UNAUTHORIZED)
                }
            }
        }
    }

}

