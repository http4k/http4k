package org.http4k.security.oauth.server.accesstoken

import com.natpryce.Failure
import com.natpryce.Success
import com.natpryce.map
import org.http4k.core.Body
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.lens.FormField
import org.http4k.lens.Validator
import org.http4k.lens.uri
import org.http4k.lens.webForm
import org.http4k.security.AccessTokenDetails
import org.http4k.security.ResponseType
import org.http4k.security.oauth.server.*
import java.time.Clock

class AuthorizationCodeAccessTokenGenerator(
    private val clientValidator: ClientValidator,
    private val authorizationCodes: AuthorizationCodes,
    private val accessTokens: AccessTokens,
    private val clock: Clock,
    private val idTokens: IdTokens
) : AccessTokenGenerator<AuthorizationCodeAccessTokenRequest> {
    override val rfcGrantType = "authorization_code"
    override fun resolveRequest(request: Request) = extract(request)

    override fun generate(request: AuthorizationCodeAccessTokenRequest) = when {
        !clientValidator.validateCredentials(request.clientId, request.clientSecret) -> Failure(InvalidClientCredentials)
        else -> {
            val code = request.authorizationCode
            val codeDetails = authorizationCodes.detailsFor(code)

            when {
                codeDetails.expiresAt.isBefore(clock.instant()) -> Failure(AuthorizationCodeExpired)
                codeDetails.clientId != request.clientId -> Failure(ClientIdMismatch)
                codeDetails.redirectUri != request.redirectUri -> Failure(RedirectUriMismatch)
                else -> accessTokens.create(code)
                    .map { token ->
                        when (codeDetails.responseType) {
                            ResponseType.Code -> AccessTokenDetails(token)
                            ResponseType.CodeIdToken -> AccessTokenDetails(token, idTokens.createForAccessToken(code))
                        }
                    }
            }
        }
    }

    companion object {
        fun extract(request: Request) = Success(AuthorizationCodeAccessTokenForm.extract(request))
    }
}

data class AuthorizationCodeAccessTokenRequest(
    val clientId: ClientId,
    val clientSecret: String,
    val redirectUri: Uri,
    val authorizationCode: AuthorizationCode
) : AccessTokenRequest

private object AuthorizationCodeAccessTokenForm {
    private val authorizationCode = FormField.map(::AuthorizationCode, AuthorizationCode::value).required("code")
    private val redirectUri = FormField.uri().required("redirect_uri")

    private val clientSecret = FormField.required("client_secret")
    private val clientId = FormField.map(::ClientId, ClientId::value).required("client_id")

    val accessTokenForm = Body.webForm(Validator.Strict,
        authorizationCode, redirectUri, clientId, clientSecret
    ).toLens()

    fun extract(request: Request): AuthorizationCodeAccessTokenRequest =
        with(accessTokenForm(request)) {
            AuthorizationCodeAccessTokenRequest(
                clientId(this),
                clientSecret(this),
                redirectUri(this),
                authorizationCode(this))
        }
}
