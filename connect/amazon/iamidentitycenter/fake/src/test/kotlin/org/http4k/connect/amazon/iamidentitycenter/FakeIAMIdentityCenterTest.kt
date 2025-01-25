package org.http4k.connect.amazon.iamidentitycenter

import org.http4k.connect.amazon.FakeAwsContract
import org.http4k.core.HttpHandler
import org.http4k.routing.reverseProxy
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable
import java.io.File
import kotlin.io.path.Path

@DisabledIfEnvironmentVariable(named = "GITHUB_ACTIONS", matches = ".*")
class FakeIAMIdentityCenterTest : IAMIdentityCenterContract, FakeAwsContract {

    init {
        //create cache directory if it doesn't exist
        Path(System.getProperty("user.home")).resolve(".aws/sso/cache").toFile().mkdirs()
    }

    override val http: HttpHandler = reverseProxy(
        "sso" to FakeSSO(),
        "oidc" to FakeOIDC()
    )
}
