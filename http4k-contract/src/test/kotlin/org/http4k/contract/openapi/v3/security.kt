package org.http4k.contract.openapi.v3

import org.http4k.contract.openapi.SecurityRendererContract
import org.http4k.contract.security.ApiKeySecurity
import org.http4k.contract.security.AuthCodeOAuthSecurity
import org.http4k.contract.security.BasicAuthSecurity
import org.http4k.contract.security.BearerAuthSecurity
import org.http4k.core.Credentials
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.lens.Query
import org.http4k.security.FakeOAuthPersistence
import org.http4k.security.OAuthProvider
import org.http4k.security.gitHub

class ApiKeySecurityRendererTest : SecurityRendererContract {
    override val security = ApiKeySecurity(Query.required("the_api_key"), { true })
    override val renderer = OpenApi3SecurityRenderer
}

class AuthCodeOAuthSecurityTest : SecurityRendererContract {
    override val security = AuthCodeOAuthSecurity(OAuthProvider.gitHub({ Response(OK) },
        Credentials("user", "password"),
        Uri.of("http://localhost/callback"),
        FakeOAuthPersistence()))
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
