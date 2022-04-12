package org.http4k.security.oauth.client

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Credentials
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.security.oauth.core.RefreshToken
import org.junit.jupiter.api.Test

class OAuthRefreshTokenTest {

    @Test
    fun `auths with correct form`() {
        val app = ClientFilters.OAuthRefreshToken(Credentials("hello", "world"), RefreshToken("goodbye"))
            .then { req: Request -> Response(Status.OK).body(req.bodyString()) }
        assertThat(
            app(Request(Method.POST, "")).bodyString(),
            equalTo("grant_type=refresh_token&client_id=hello&client_secret=world&refresh_token=goodbye")
        )
    }
}
