package org.http4k.security.oauth.server

import org.http4k.security.oauth.server.RfcError.InvalidClient
import org.http4k.security.oauth.server.RfcError.InvalidGrant


abstract class OAuthError(val rfcError: RfcError, val description: String)

enum class RfcError {
    AccessDenied,
    InvalidClient,
    InvalidGrant,
    UnsupportedGrantType,
    UnsupportedResponseType;

    val rfcValue
        get() = when (this) {
            InvalidClient -> "invalid_client"
            InvalidGrant -> "invalid_grant"
            UnsupportedGrantType -> "unsupported_grant_type"
            UnsupportedResponseType -> "unsupported_response_type"
            AccessDenied -> "access_denied"
        }
}

// represents errors according to https://tools.ietf.org/html/rfc6749#section-5.2
sealed class AccessTokenError(rfcError: RfcError, description: String) : OAuthError(rfcError, description)

data class UnsupportedGrantType(val requestedGrantType: String) : AccessTokenError(RfcError.UnsupportedGrantType, "$requestedGrantType is not supported")
object InvalidClientCredentials : AccessTokenError(InvalidClient, "The 'client_id' parameter does not match the authorization request")
object AuthorizationCodeExpired : AccessTokenError(InvalidGrant, "The authorization code has expired")
object ClientIdMismatch : AccessTokenError(InvalidGrant, "The 'client_id' parameter does not match the authorization request")
object RedirectUriMismatch : AccessTokenError(InvalidGrant, "The 'redirect_uri' parameter does not match the authorization request")
object AuthorizationCodeAlreadyUsed : AccessTokenError(InvalidGrant, "The authorization code has already been used")

// represents errors according to https://tools.ietf.org/html/rfc6749#section-4.1.2.1
sealed class AuthorizationError(rfcError: RfcError, description: String) : OAuthError(rfcError, description)

object UserRejectedRequest : AuthorizationError(RfcError.AccessDenied, "The user declined the authorization request")
object InvalidClientId : AuthorizationError(InvalidClient, "The specified client id is invalid")
object InvalidRedirectUri : AuthorizationError(InvalidClient, "The specified redirect uri is not registered")
data class UnsupportedResponseType(val requestedResponseType: String): AuthorizationError(RfcError.UnsupportedResponseType, "The specified response_type '$requestedResponseType' is not supported")
