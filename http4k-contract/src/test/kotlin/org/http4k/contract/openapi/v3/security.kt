package org.http4k.contract.openapi.v3

import org.http4k.contract.openapi.SecurityRenderer
import org.http4k.contract.openapi.SecurityRendererContract
import org.http4k.contract.security.ApiKeySecurity
import org.http4k.contract.security.AuthCodeOAuthSecurity
import org.http4k.contract.security.BasicAuthSecurity
import org.http4k.contract.security.BearerAuthSecurity
import org.http4k.contract.security.ImplicitOAuthSecurity
import org.http4k.contract.security.OAuthScope
import org.http4k.contract.security.OpenIdConnectSecurity
import org.http4k.contract.security.Security
import org.http4k.contract.security.UserCredentialsOAuthSecurity
import org.http4k.core.Credentials
import org.http4k.core.Filter
import org.http4k.core.NoOp
import org.http4k.core.Uri
import org.http4k.lens.Query

class ApiKeySecurityRendererTest : SecurityRendererContract {
    override val security = ApiKeySecurity(Query.required("the_api_key"), { true })
    override val renderer = OpenApi3SecurityRenderer
}

class AuthCodeOAuthSecurityRendererTest : SecurityRendererContract {
    override val security = AuthCodeOAuthSecurity(
        Uri.of("/auth"),
        Uri.of("/token"),
        listOf(OAuthScope("name", "value")),
        Filter.NoOp,
        "custom",
        Uri.of("/refresh"),
        mapOf("extra1" to "value2")
    )

    override val renderer = OpenApi3SecurityRenderer
}

class BasicSecurityRendererTest : SecurityRendererContract {
    override val security = BasicAuthSecurity("realm", Credentials("user", "password"))
    override val renderer = OpenApi3SecurityRenderer
}

class BearerAuthSecurityRendererTest : SecurityRendererContract {
    override val security = BearerAuthSecurity("foo")
    override val renderer = OpenApi3SecurityRenderer
}

class ImplicitOAuthSecurityRendererTest : SecurityRendererContract {
    override val security = ImplicitOAuthSecurity(
        Uri.of("/auth"),
        listOf(OAuthScope("name", "value")),
        Filter.NoOp,
        "custom",
        Uri.of("/refresh"),
        mapOf("extra1" to "value2")
    )

    override val renderer = OpenApi3SecurityRenderer
}

class UserCredentialsSecurityRendererTest: SecurityRendererContract {
    override val security = UserCredentialsOAuthSecurity(
        Uri.of("/auth"),
        listOf(OAuthScope("name", "value")),
        Filter.NoOp,
        "custom",
        Uri.of("/refresh"),
        mapOf("extra1" to "value2")
    )

    override val renderer = OpenApi3SecurityRenderer
}

class OpenIdConnectSecurityRendererTest: SecurityRendererContract {
    override val security = OpenIdConnectSecurity(
        Uri.of("spec"),
        Filter.NoOp,
        "customOidc"
    )

    override val renderer = OpenApi3SecurityRenderer
}
