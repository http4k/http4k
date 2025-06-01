package org.http4k.a2a.protocol.model

import org.http4k.core.Uri
import se.ansman.kotshi.JsonSerializable
import se.ansman.kotshi.Polymorphic
import se.ansman.kotshi.PolymorphicLabel

@JsonSerializable
@Polymorphic("type")
sealed interface SecurityScheme {
    val description: String?
}

@JsonSerializable
@PolymorphicLabel("apiKey")
data class APIKeySecurityScheme(
    val name: String,
    val `in`: APIKeyLocation,
    override val description: String? = null
) : SecurityScheme

@JsonSerializable
enum class APIKeyLocation {
    query,
    header,
    cookie
}

@JsonSerializable
@PolymorphicLabel("http")
data class HTTPAuthSecurityScheme(
    val scheme: String,
    val bearerFormat: String? = null,
    override val description: String? = null
) : SecurityScheme

@JsonSerializable
@PolymorphicLabel("oauth2")
data class OAuth2SecurityScheme(
    val flows: OAuthFlows,
    override val description: String? = null
) : SecurityScheme

@JsonSerializable
@PolymorphicLabel("openIdConnect")
data class OpenIdConnectSecurityScheme(
    val openIdConnectUrl: String,
    override val description: String? = null
) : SecurityScheme

@JsonSerializable
data class OAuthFlows(
    val implicit: ImplicitOAuthFlow? = null,
    val password: PasswordOAuthFlow? = null,
    val clientCredentials: ClientCredentialsOAuthFlow? = null,
    val authorizationCode: AuthorizationCodeOAuthFlow? = null
)

@JsonSerializable
data class ImplicitOAuthFlow(
    val authorizationUrl: Uri,
    val scopes: Map<String, String>,
    val refreshUrl: String? = null
)

@JsonSerializable
data class PasswordOAuthFlow(
    val tokenUrl: Uri,
    val scopes: Map<String, String>,
    val refreshUrl: String? = null
)

@JsonSerializable
data class ClientCredentialsOAuthFlow(
    val tokenUrl: Uri,
    val scopes: Map<String, String>,
    val refreshUrl: String? = null
)

@JsonSerializable
data class AuthorizationCodeOAuthFlow(
    val authorizationUrl: Uri,
    val tokenUrl: Uri,
    val scopes: Map<String, String>,
    val refreshUrl: Uri? = null
)
