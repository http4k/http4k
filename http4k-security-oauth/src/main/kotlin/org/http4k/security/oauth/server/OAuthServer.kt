package org.http4k.security.oauth.server

import org.http4k.core.*
import org.http4k.core.Method.POST
import org.http4k.core.Status.Companion.OK
import org.http4k.core.cookie.Cookie
import org.http4k.core.cookie.cookie
import org.http4k.core.cookie.invalidateCookie
import org.http4k.lens.Query
import org.http4k.lens.uri
import org.http4k.lens.uuid
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.security.oauth.server.OAuthServer.Companion.clientId
import org.http4k.security.oauth.server.OAuthServer.Companion.redirectUri
import org.http4k.security.oauth.server.OAuthServer.Companion.scopes
import org.http4k.security.oauth.server.OAuthServer.Companion.state
import java.util.*

class OAuthServer(
        tokenPath: String,
        private val validateClientAndRedirectionUri: ClientValidator,
        private val generateAuthorizationCode: AuthorizationCodeGenerator,
        private val persistence: OAuthRequestPersistence
) {
    val tokenRoute = routes(tokenPath bind POST to {
        Response(OK).body("an-access-token")
    })

    val authenticationStart = Filter { next ->
        {
            val authorizationRequest = it.authorizationRequest()
            if (!validateClientAndRedirectionUri(authorizationRequest.client, authorizationRequest.redirectUri)) {
                Response(Status.BAD_REQUEST)
            } else {
                persistence.store(authorizationRequest, next(it))
            }
        }
    }

    val authenticationComplete = Filter { next ->
        { request ->
            val response = next(request)
            val authorizationRequest = persistence.retrieve(request)
            val postAuthResponse = if (response.status.successful) {
                Response(Status.TEMPORARY_REDIRECT)
                        .header("location", authorizationRequest.redirectUri
                                .query("code", generateAuthorizationCode().value)
                                .query("state", authorizationRequest.state)
                                .toString())
            } else response
            persistence.clear(authorizationRequest, postAuthResponse)
        }
    }

    companion object {
        val clientId = Query.map(::ClientId, ClientId::value).required("client_id")
        val scopes = Query.map({ it.split(",").toList() }, { it.joinToString(",") }).optional("scopes")
        val redirectUri = Query.uri().required("redirect_uri")
        val state = Query.optional("state")
    }
}

interface OAuthRequestPersistence {
    fun store(authorizationRequest: AuthorizationRequest, response: Response): Response
    fun retrieve(request: Request): AuthorizationRequest
    fun clear(authorizationRequest: AuthorizationRequest, response: Response): Response
}

typealias ClientValidator = (ClientId, Uri) -> Boolean
typealias AuthorizationCodeGenerator = () -> AuthorizationCode

class InsecureCookieBasedOAuthRequestPersistence : OAuthRequestPersistence {
    private val cookieName = "OauthRequest"

    override fun store(authorizationRequest: AuthorizationRequest, response: Response): Response {
        val parameters = DUMMY
                .with(id of authorizationRequest.id)
                .with(clientId of authorizationRequest.client)
                .with(scopes of authorizationRequest.scopes)
                .with(redirectUri of authorizationRequest.redirectUri)
                .with(state of authorizationRequest.state)
        return response.cookie(Cookie(cookieName, parameters.uri.query))
    }

    override fun retrieve(request: Request): AuthorizationRequest {
        val parameters = DUMMY.uri(DUMMY.uri.query(request.cookie(cookieName)?.value.orEmpty()))
        return parameters.authorizationRequest(id(parameters))
    }

    override fun clear(authorizationRequest: AuthorizationRequest, response: Response): Response =
            response.invalidateCookie(cookieName)

    companion object {
        private val DUMMY = Request(Method.GET, Uri.of("/dummy"))
        val id = Query.uuid().required("id")
    }
}

data class AuthorizationRequest(
        val id: UUID,
        val client: ClientId,
        val scopes: List<String>,
        val redirectUri: Uri,
        val state: String?
)

internal fun Request.authorizationRequest(id: UUID = UUID.randomUUID()) =
        AuthorizationRequest(
                id,
                clientId(this),
                scopes(this) ?: listOf(),
                redirectUri(this),
                state(this)
        )

data class ClientId(val value: String)
data class AuthorizationCode(val value: String)