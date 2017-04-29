package org.reekwest.http.contract.module

import org.reekwest.http.core.Filter
import org.reekwest.http.core.Request
import org.reekwest.http.core.Response
import org.reekwest.http.core.Status.Companion.UNAUTHORIZED
import org.reekwest.http.lens.Lens
import org.reekwest.http.lens.LensFailure


/**
 * Endpoint security. Provides filter to be applied to endpoints for all requests.
 */
sealed class Security {
    abstract val filter: Filter
}

/**
 * Default NoOp security filter. Filter allows all traffic through.
 */
object NoSecurity : Security() {
    override val filter = Filter { it }
}

/**
 * Checks the presence of the named Api Key parameter. Filter returns 401 if Api-Key is not found in request.
 */
data class ApiKey<T>(val param: Lens<Request, T>, val validateKey: (T) -> Boolean) : Security() {
    override val filter = Filter {
        next ->
        {
            try {
                if (validateKey(param(it))) next(it) else Response(UNAUTHORIZED)
            } catch (e: LensFailure) {
                Response(UNAUTHORIZED)
            }
        }
    }
}

