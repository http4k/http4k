package org.http4k.contract

import org.http4k.core.Request
import org.http4k.lens.BodyLens
import org.http4k.lens.LensExtractor

/**
 * Determines which parts of the request should be pre-extracted to check for presence before being passed to the ultimate
 * HttpHandler for this route. Choice will be determined by two competing scenarios:
 *
 * 1. By selecting a All of the request to be pre-extracted (the default), we can collect all errors at once to be
 * returned to the client - which is more user friendly. If we fall back on just the Lens usage in the
 * HttpHandler code, only the first failing Lens extraction will be reported.
 * 2. Not pre-checking parts of the request is more efficient, which may be important given parsing of request
 * bodies could be expensive and pre-flight-extraction would involve performing this operation twice.
 */
fun interface PreFlightExtraction : (RouteMeta) -> List<LensExtractor<Request, *>> {
    companion object {

        /**
         * Check the entire contract, including extracting the body, before passing it to the underlying
         * HttpHandler.
         */
        val All = PreFlightExtraction {
            it.requestParams + (it.body?.let { listOf(it) }
                ?: emptyList<BodyLens<*>>())
        }

        /**
         * Check all parts of the contract apart from the body, relying on the HttpHandler code to raise a correct
         * LensFailure if extraction fails. Use this option to avoid re-extracting the body multiple times.
         */
        val IgnoreBody = PreFlightExtraction { it.requestParams }

        /**
         * Check none the contract, relying entirely  on the HttpHandler code to raise a correct
         * LensFailure if extraction fails. Use this option to fully optimise performance, at the risk
         * of not checking
         */
        val None = PreFlightExtraction { emptyList() }
    }
}
