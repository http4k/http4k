package org.http4k.connect.amazon.iamidentitycenter

import org.http4k.connect.amazon.FakeAwsContract
import org.http4k.core.HttpHandler
import org.http4k.routing.reverseProxy

class FakeIAMIdentityCenterTest : IAMIdentityCenterContract, FakeAwsContract {

    override val http: HttpHandler = reverseProxy(
        "sso" to FakeSSO(),
        "oidc" to FakeOIDC()
    )
}
