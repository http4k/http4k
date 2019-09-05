package org.http4k.security.oauth.server.accesstoken

import com.natpryce.Failure
import com.natpryce.Result
import com.natpryce.Success
import com.natpryce.flatMap
import com.natpryce.map
import org.http4k.core.Body
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.lens.FormField
import org.http4k.lens.Validator
import org.http4k.lens.uri
import org.http4k.lens.webForm
import org.http4k.security.AccessTokenDetails
import org.http4k.security.oauth.server.AccessTokenError
import org.http4k.security.oauth.server.AccessTokens
import org.http4k.security.oauth.server.AuthorizationCode
import org.http4k.security.oauth.server.AuthorizationCodeExpired
import org.http4k.security.oauth.server.AuthorizationCodes
import org.http4k.security.oauth.server.ClientId
import org.http4k.security.oauth.server.ClientIdMismatch
import org.http4k.security.oauth.server.IdTokens
import org.http4k.security.oauth.server.RedirectUriMismatch
import java.time.Clock

class AuthorizationCodeAccessTokenGenerator(
    private val authorizationCodes: AuthorizationCodes,
    private val accessTokens: AccessTokens,
    private val clock: Clock,
    private val idTokens: IdTokens
) : AccessTokenGenerator {
    override fun generate(request: Request) =
        extract(request).flatMap { generate(it) }

    fun generate(request: AuthorizationCodeAccessTokenRequest): Result<AccessTokenDetails, AccessTokenError> {
        val code = request.authorizationCode
        val codeDetails = authorizationCodes.detailsFor(code)

        return when {
            codeDetails.expiresAt.isBefore(clock.instant()) -> Failure(AuthorizationCodeExpired)
            codeDetails.clientId != request.clientId -> Failure(ClientIdMismatch)
            codeDetails.redirectUri != request.redirectUri -> Failure(RedirectUriMismatch)
            else -> accessTokens.create(code)
                .map { token ->
                    when {
                        codeDetails.isOIDC -> AccessTokenDetails(token, idTokens.createForAccessToken(codeDetails, code, token))
                        else -> AccessTokenDetails(token)
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
)

private object AuthorizationCodeAccessTokenForm {
    private val authorizationCode = FormField.map(::AuthorizationCode, AuthorizationCode::value).required("code")
    private val redirectUri = FormField.uri().required("redirect_uri")

    private val clientSecret = FormField.optional("client_secret")
    private val clientId = FormField.map(::ClientId, ClientId::value).required("client_id")

    val accessTokenForm = Body.webForm(Validator.Strict,
        authorizationCode, redirectUri, clientId, clientSecret
    ).toLens()

    fun extract(request: Request): AuthorizationCodeAccessTokenRequest =
        with(accessTokenForm(request)) {
            AuthorizationCodeAccessTokenRequest(
                clientId(this),
                clientSecret(this) ?: "",
                redirectUri(this),
                authorizationCode(this))
        }
}
