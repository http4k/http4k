package org.http4k.contract.security

import org.http4k.security.OAuthProvider
import org.http4k.security.OAuthScope

@Deprecated("Moved module", ReplaceWith("org.http4k.security.ApiKeySecurity<T>"))
typealias ApiKeySecurity<T> = org.http4k.security.ApiKeySecurity<T>

@Deprecated("Moved module", ReplaceWith("org.http4k.security.Security"))
typealias Security = org.http4k.security.Security

@Deprecated("Moved module", ReplaceWith("org.http4k.security.BearerAuthSecurity"))
typealias BearerAuthSecurity = org.http4k.security.BearerAuthSecurity

@Deprecated("Moved module", ReplaceWith("org.http4k.security.BasicAuthSecurity"))
typealias BasicAuthSecurity = org.http4k.security.BasicAuthSecurity

@Deprecated("Moved module", ReplaceWith("org.http4k.security.NoSecurity"))
typealias NoSecurity = org.http4k.security.NoSecurity

@Deprecated("Moved module", ReplaceWith("org.http4k.security.OAuthSecurity"))
typealias OAuthSecurity = org.http4k.security.OAuthSecurity

@Deprecated("Moved module", ReplaceWith("org.http4k.security.AuthCodeOAuthSecurity"))
typealias AuthCodeOAuthSecurity = org.http4k.security.AuthCodeOAuthSecurity

@Deprecated("Moved module", ReplaceWith("org.http4k.security.ImplicitOAuthSecurity"))
typealias ImplicitOAuthSecurity = org.http4k.security.ImplicitOAuthSecurity

@Deprecated("Moved module", ReplaceWith("org.http4k.security.UserCredentialsOAuthSecurity"))
typealias UserCredentialsOAuthSecurity = org.http4k.security.UserCredentialsOAuthSecurity

@Deprecated("Moved module", ReplaceWith("org.http4k.security.ClientCredentialsOAuthSecurity"))
typealias ClientCredentialsOAuthSecurity = org.http4k.security.ClientCredentialsOAuthSecurity

@Deprecated("Moved module", ReplaceWith("org.http4k.security.OpenIdConnectSecurity"))
typealias OpenIdConnectSecurity = org.http4k.security.OpenIdConnectSecurity

@Deprecated("Moved module", ReplaceWith("org.http4k.security.OAuthScope"))
typealias OAuthScope = org.http4k.security.OAuthScope

@Deprecated("Moved module", ReplaceWith("org.http4k.security.AuthCodeOAuthSecurity"))
fun AuthCodeOAuthSecurity(
    oAuthProvider: OAuthProvider,
    customScopes: List<OAuthScope>? = null
) = org.http4k.security.AuthCodeOAuthSecurity(oAuthProvider, customScopes)
