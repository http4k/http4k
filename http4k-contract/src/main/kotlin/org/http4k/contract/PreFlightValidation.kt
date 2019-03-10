package org.http4k.contract

import org.http4k.core.Request
import org.http4k.lens.BodyLens
import org.http4k.lens.LensExtractor

/**
 * Determines which parts of the request should be pre-validated before being passed to the ultimate
 * HttpHandler for this route. Choice will be determined by two competing scenarios:
 *
 * 1. By selecting a part of the request to be pre-validated, we can collect all errors at once to be
 * returned to the client - which is more user friendly. If we fall back on just the Lens usage in the
 * HttpHandler code, only the first failing Lens extraction will be reported.
 * 2. Not pre-validating the request is more efficient, which may be important given parsing of request
 * bodies could be expensive and pre-validation involves performing this operation twice.
 *
 * Violations will result in a Lens failure and a BAD_REQUEST being returned.
 */
interface PreFlightValidation : (RouteMeta) -> List<LensExtractor<Request, *>> {
    companion object {

        object All : PreFlightValidation {
            override fun invoke(meta: RouteMeta) = BodyOnly(meta) + NonBodyOnly(meta)
        }

        object BodyOnly : PreFlightValidation {
            override fun invoke(meta: RouteMeta) = meta.body()
        }

        object NonBodyOnly : PreFlightValidation {
            override fun invoke(meta: RouteMeta) = meta.requestParams
        }

        object None : PreFlightValidation {
            override fun invoke(meta: RouteMeta) = emptyList<BodyLens<*>>()
        }

        private fun RouteMeta.body() = (body?.let { listOf(it) } ?: emptyList())
    }
}