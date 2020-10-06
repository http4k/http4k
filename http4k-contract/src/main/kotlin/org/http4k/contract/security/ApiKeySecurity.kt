package org.http4k.contract.security

import org.http4k.core.Filter
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.lens.Lens
import org.http4k.lens.LensFailure

/**
 * Checks the presence of the named Api Key parameter. Filter returns 401 if Api-Key is not found in request.
 *
 * Default implementation of ApiKey. Includes an option to NOT authorise OPTIONS requests, which is
 * currently not enabled for OpenAPI.
 */
class ApiKeySecurity<out T>(
    val param: Lens<Request, T>,
    validateKey: (T) -> Boolean,
    authorizeOptionsRequests: Boolean = true,
    val name: String = "api_key"
) : Security {
    override val filter = Filter { next ->
        {
            if (!authorizeOptionsRequests && it.method == Method.OPTIONS) {
                next(it)
            } else {
                val keyValid = try {
                    validateKey(param(it))
                } catch (e: LensFailure) {
                    false
                }
                if (keyValid) next(it) else Response(UNAUTHORIZED)
            }
        }
    }

    companion object
}
