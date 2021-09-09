package org.http4k.security.oauth.server

import dev.forkhandles.result4k.get
import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.mapFailure
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.security.ResponseType.Code
import org.http4k.security.ResponseType.CodeIdToken

class AuthenticationComplete(
    private val authorizationCodes: AuthorizationCodes,
    private val requestTracking: AuthRequestTracking,
    private val idTokens: IdTokens,
    private val documentationUri: String? = null
) : HttpHandler {

    override fun invoke(request: Request): Response {
        val authorizationRequest = requestTracking.resolveAuthRequest(request)
            ?: error("Authorization request could not be found.")

        return ResponseRender
            .forAuthRequest(authorizationRequest).addResponseTypeValues(authorizationRequest, request)
            .withState(authorizationRequest.state)
            .complete()
    }

    private fun ResponseRender.addResponseTypeValues(
        authorizationRequest: AuthRequest,
        request: Request,
        response: Response = this.complete()
    ): ResponseRender =
        with(authorizationCodes.create(request, authorizationRequest, response)) {
            map {
                when (authorizationRequest.responseType) {
                    Code -> addParameter("code", it.value)
                    CodeIdToken -> addParameter("code", it.value)
                        .addParameter(
                            "id_token",
                            idTokens.createForAuthorization(
                                request,
                                authorizationRequest,
                                response,
                                authorizationRequest.nonce,
                                it
                            ).value
                        )
                }
            }
                .mapFailure {
                    val responseRender = addParameter("error", it.rfcError.rfcValue)
                        .addParameter("error_description", it.description)
                    documentationUri?.addTo(responseRender) ?: responseRender
                }
                .get()
        }

    private fun String.addTo(responseRender: ResponseRender): ResponseRender =
        responseRender.addParameter("error_uri", this)
}
