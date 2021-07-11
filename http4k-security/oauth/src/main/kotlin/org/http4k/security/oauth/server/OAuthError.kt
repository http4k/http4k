package org.http4k.security.oauth.server

import org.http4k.security.oauth.server.RfcError.InvalidClient
import org.http4k.security.oauth.server.RfcError.InvalidGrant
import org.http4k.security.oauth.server.RfcError.InvalidRequest
import org.http4k.security.oauth.server.RfcError.InvalidRequestObject
import org.http4k.security.oauth.server.RfcError.InvalidScope

abstract class OAuthError(val rfcError: RfcError, val description: String)

enum class RfcError {
    AccessDenied,
    InvalidClient,
    InvalidRequest,
    InvalidRequestObject,
    InvalidGrant,
    InvalidScope,
    UnsupportedGrantType,
    UnsupportedResponseType;

    val rfcValue
        get() = when (this) {
            InvalidClient -> "invalid_client"
            InvalidGrant -> "invalid_grant"
            InvalidScope -> "invalid_scope"
            UnsupportedGrantType -> "unsupported_grant_type"
            UnsupportedResponseType -> "unsupported_response_type"
            AccessDenied -> "access_denied"
            InvalidRequest -> "invalid_request"
            InvalidRequestObject -> "invalid_request_object"
        }
}

// represents errors according to https://tools.ietf.org/html/rfc6749#section-5.2
sealed class AccessTokenError(rfcError: RfcError, description: String) : OAuthError(rfcError, description)

data class UnsupportedGrantType(val requestedGrantType: String) : AccessTokenError(RfcError.UnsupportedGrantType, "$requestedGrantType is not supported")
object InvalidClientAssertionType : AccessTokenError(InvalidGrant, "The 'client_assertion_type' is invalid")
object InvalidClientAssertion : AccessTokenError(InvalidGrant, "The 'client_assertion' is invalid")
object InvalidClientCredentials : AccessTokenError(InvalidClient, "The 'client_id' parameter does not match the authorization request")
object AuthorizationCodeExpired : AccessTokenError(InvalidGrant, "The authorization code has expired")
object ClientIdMismatch : AccessTokenError(InvalidGrant, "The 'client_id' parameter does not match the authorization request")
object RedirectUriMismatch : AccessTokenError(InvalidGrant, "The 'redirect_uri' parameter is required")
object MissingRedirectUri : AccessTokenError(InvalidGrant, "The 'redirect_uri' parameter does not match the authorization request")
object AuthorizationCodeAlreadyUsed : AccessTokenError(InvalidGrant, "The authorization code has already been used")
object MissingAuthorizationCode : AccessTokenError(InvalidGrant, "The authorization code is required")
data class InvalidRequest(val message: String) : AccessTokenError(InvalidRequest, message)

// represents errors according to https://tools.ietf.org/html/rfc6749#section-4.1.2.1
sealed class AuthorizationError(rfcError: RfcError, description: String) : OAuthError(rfcError, description)

object UserRejectedRequest : AuthorizationError(RfcError.AccessDenied, "The user declined the authorization request")
object InvalidClientId : AuthorizationError(InvalidClient, "The specified client id is invalid")
object InvalidRedirectUri : AuthorizationError(InvalidClient, "The specified redirect uri is not registered")
object InvalidScopes : AuthorizationError(InvalidScope, "The specified scopes are invalid")
object InvalidRequestObject : AuthorizationError(InvalidRequestObject, "The specified request is invalid")
data class UnsupportedResponseType(val requestedResponseType: String) : AuthorizationError(RfcError.UnsupportedResponseType, "The specified response_type '$requestedResponseType' is not supported")
data class InvalidAuthorizationRequest(val reason: String) : AuthorizationError(InvalidRequest, reason)
