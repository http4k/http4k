package org.http4k.filter

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.then

/**
 * Provides CORs plus blocking rebind protection for HTTP requests.
 */
fun ServerFilters.HttpRebindProtection(corsPolicy: CorsPolicy): Filter = Filter { next ->
    val cors = ServerFilters.Cors(corsPolicy).then(next)
    val handler: HttpHandler = { req ->
        val origin = req.header("Origin")
        when {
            req.method != Method.OPTIONS && origin != null && !corsPolicy.originPolicy(origin) -> Response.Companion(
                Status.FORBIDDEN
            )

            else -> cors(req)
        }
    }
    handler
}
