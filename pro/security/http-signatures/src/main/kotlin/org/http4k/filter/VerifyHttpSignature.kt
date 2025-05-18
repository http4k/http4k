package org.http4k.filter

import dev.forkhandles.result4k.get
import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.mapFailure
import dev.forkhandles.result4k.onFailure
import org.http4k.core.Filter
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.security.signature.HttpMessageSignatureVerifier
import org.http4k.security.signature.VerificationError.InvalidSignatureHeader
import org.http4k.security.signature.VerificationError.OtherError
import org.http4k.security.signature.VerificationError.SignatureBaseCreationError

/**
 * Verifies RFC-9421 HTTP signatures on incoming requests
 */
fun ServerFilters.VerifyHttpSignature(verifier: HttpMessageSignatureVerifier<Request, *, *>) =
    Filter { next ->
        { request ->
            verifier(request, request)
                .map { next(request) }
                .mapFailure {
                    when (it) {
                        is InvalidSignatureHeader, is SignatureBaseCreationError, is OtherError -> Response(BAD_REQUEST)
                        else -> Response(UNAUTHORIZED)
                    }
                }
                .get()
        }
    }

/**
 * Verifies RFC-9421 HTTP signatures on received responses
 */
fun ClientFilters.VerifyHttpSignature(verifier: HttpMessageSignatureVerifier<Response, *, *>) =
    Filter { next ->
        { req ->
            next(req).let { resp ->
                verifier(
                    req,
                    resp
                ).onFailure { throw IllegalStateException("Failed to verify response signature: $it") }
                resp
            }
        }
    }
