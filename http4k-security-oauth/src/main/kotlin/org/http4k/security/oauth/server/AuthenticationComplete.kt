package org.http4k.security.oauth.server

import com.natpryce.get
import com.natpryce.map
import com.natpryce.mapFailure
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.SEE_OTHER
import org.http4k.core.Uri
import org.http4k.core.query
import org.http4k.security.ResponseType.Code
import org.http4k.security.ResponseType.CodeIdToken

class AuthenticationComplete(
    private val authorizationCodes: AuthorizationCodes,
    private val requestTracking: AuthRequestTracking,
    private val idTokens: IdTokens,
    private val documentationUri: String? = null) : HttpHandler {

    override fun invoke(request: Request): Response {
        val response = Response(SEE_OTHER)
        val authorizationRequest = requestTracking.resolveAuthRequest(request)
            ?: error("Authorization request could not be found.")

        return Response(SEE_OTHER)
            .header("location", authorizationRequest.redirectUri
                .addResponseTypeValues(authorizationRequest, request, response)
                .query("state", authorizationRequest.state)
                .toString())
    }

    private fun Uri.addResponseTypeValues(authorizationRequest: AuthRequest, request: Request, response: Response): Uri =
        with(authorizationCodes.create(request, authorizationRequest, response)) {
            map {
                when (authorizationRequest.responseType) {
                    Code -> query("code", it.value)
                    CodeIdToken -> query("code", it.value)
                        .query("id_token", idTokens.createForAuthorization(request, authorizationRequest, response, it).value)
                }
            }
                .mapFailure {
                    val uri = query("error", it.rfcError.rfcValue)
                        .query("error_description", it.description)
                    documentationUri?.addTo(uri) ?: uri
                }
                .get()
        }

    private fun String.addTo(uri: Uri): Uri = uri.query("error_uri", this)
}