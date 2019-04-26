package org.http4k.security.oauth.server

import org.http4k.security.oauth.server.RfcError.InvalidClient
import org.http4k.security.oauth.server.RfcError.InvalidGrant


abstract class OAuthError(val rfcError: RfcError, val description: String)

enum class RfcError {
    InvalidClient,
    InvalidGrant,
    UnsupportedGrantType
}

// represents errors according to https://tools.ietf.org/html/rfc6749#section-5.2
sealed class AccessTokenError(rfcError: RfcError, description: String) : OAuthError(rfcError, description)

data class UnsupportedGrantType(val requestedGrantType: String) : AccessTokenError(RfcError.UnsupportedGrantType, "$requestedGrantType is not supported")
object InvalidClientCredentials : AccessTokenError(InvalidClient, "The 'client_id' parameter does not match the authorization request")
object AuthorizationCodeExpired : AccessTokenError(InvalidGrant, "The authorization code has expired")
object ClientIdMismatch : AccessTokenError(InvalidGrant, "The 'client_id' parameter does not match the authorization request")
object RedirectUriMismatch : AccessTokenError(InvalidGrant, "The 'redirect_uri' parameter does not match the authorization request")
object AuthorizationCodeAlreadyUsed : AccessTokenError(InvalidGrant, "The authorization code has already been used")

object InvalidClientId : OAuthError(InvalidClient, "The specified client id is invalid")
object InvalidRedirectUri : OAuthError(InvalidClient, "The specified redirect uri is not registered")
