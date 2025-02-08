package org.http4k.contract.openapi.v3

import org.http4k.contract.openapi.SecurityRendererContract
import org.http4k.core.Credentials
import org.http4k.core.Filter
import org.http4k.core.NoOp
import org.http4k.core.Uri
import org.http4k.lens.Header
import org.http4k.lens.Query
import org.http4k.lens.string
import org.http4k.security.ApiKeySecurity
import org.http4k.security.ClientCredentialsOAuthSecurity
import org.http4k.security.ImplicitOAuthSecurity
import org.http4k.security.OAuthScope
import org.http4k.security.OpenIdConnectSecurity
import org.http4k.security.UserCredentialsOAuthSecurity

class ApiKeySecurityRendererTest : SecurityRendererContract {
    override val security = ApiKeySecurity(Query.required("the_api_key"), { true })
    override val renderer = OpenApi3SecurityRenderer
}

@Suppress("DEPRECATION")
class ApiKeySecurityWithConsumerRendererTest : SecurityRendererContract {
    override val security =
        ApiKeySecurity(Query.required("the_api_key"), Header.string().required("consumer-name"), { "" })
    override val renderer = OpenApi3SecurityRenderer
}

class AuthCodeOAuthSecurityRendererTest : SecurityRendererContract {
    override val security = org.http4k.security.AuthCodeOAuthSecurity(
        Uri.of("/auth"),
        Uri.of("/token"),
        listOf(org.http4k.security.OAuthScope("name", "value")),
        Filter.NoOp,
        "custom",
        Uri.of("/refresh"),
        mapOf("extra1" to "value2")
    )

    override val renderer = OpenApi3SecurityRenderer
}

class BasicSecurityRendererTest : SecurityRendererContract {
    override val security = org.http4k.security.BasicAuthSecurity("realm", Credentials("user", "password"))
    override val renderer = OpenApi3SecurityRenderer
}

class BearerAuthSecurityRendererTest : SecurityRendererContract {
    override val security = org.http4k.security.BearerAuthSecurity("foo")
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

class UserCredentialsSecurityRendererTest : SecurityRendererContract {
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

class ClientCredentialsSecurityRendererTest : SecurityRendererContract {
    override val security = ClientCredentialsOAuthSecurity(
        Uri.of("/auth"),
        listOf(OAuthScope("name", "value")),
        Filter.NoOp,
        "custom",
        Uri.of("/refresh"),
        mapOf("extra1" to "value2")
    )

    override val renderer = OpenApi3SecurityRenderer
}

class OpenIdConnectSecurityRendererTest : SecurityRendererContract {
    override val security = OpenIdConnectSecurity(
        Uri.of("spec"),
        Filter.NoOp,
        "customOidc"
    )

    override val renderer = OpenApi3SecurityRenderer
}
