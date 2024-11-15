package org.http4k.connect.amazon.cognito.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.cognito.CognitoAction
import org.http4k.connect.amazon.cognito.model.AnalyticsConfiguration
import org.http4k.connect.amazon.cognito.model.ClientName
import org.http4k.connect.amazon.cognito.model.ExplicitAuthFlow
import org.http4k.connect.amazon.cognito.model.OAuthFlow
import org.http4k.connect.amazon.cognito.model.TokenValidityUnits
import org.http4k.connect.amazon.cognito.model.UserPoolClient
import org.http4k.connect.amazon.cognito.model.UserPoolId
import org.http4k.core.Uri
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
@JsonSerializable
data class CreateUserPoolClient(
    val UserPoolId: UserPoolId,
    val ClientName: ClientName,
    val AllowedOAuthFlows: List<OAuthFlow>? = null,
    val AccessTokenValidity: Int? = null,
    val AllowedOAuthFlowsUserPoolClient: Boolean? = null,
    val AllowedOAuthScopes: List<String>? = null,
    val AnalyticsConfiguration: AnalyticsConfiguration? = null,
    val CallbackURLs: List<Uri>? = null,
    val DefaultRedirectURI: Uri? = null,
    val ExplicitAuthFlows: List<ExplicitAuthFlow>? = null,
    val GenerateSecret: Boolean? = null,
    val IdTokenValidity: Int? = null,
    val LogoutURLs: List<Uri>? = null,
    val PreventUserExistenceErrors: String? = null,
    val ReadAttributes: List<String>? = null,
    val RefreshTokenValidity: Int? = null,
    val SupportedIdentityProviders: List<String>? = null,
    val TokenValidityUnits: TokenValidityUnits? = null,
    val WriteAttributes: List<String>? = null
) : CognitoAction<CreatedUserPoolClient>(CreatedUserPoolClient::class)

@JsonSerializable
data class CreatedUserPoolClient(
    val UserPoolClient: UserPoolClient
)
