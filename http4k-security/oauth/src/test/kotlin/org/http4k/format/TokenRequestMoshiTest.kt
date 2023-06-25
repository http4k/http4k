package org.http4k.format

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.with
import org.http4k.security.oauth.client.TokenRequest
import org.http4k.security.oauth.client.tokenRequestLens
import org.http4k.testing.Approver
import org.http4k.testing.JsonApprovalTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(JsonApprovalTest::class)
class TokenRequestMoshiTest {

    private val marshaller = OAuthMoshi

    @Test
    fun `deserialize with explicit nulls`() {
        val json = """{
            "grant_type":"authorization_code",
            "refresh_token":null,
            "client_id":null,
            "code":null,
            "redirect_uri":null
        }"""

        assertThat(marshaller.asA(json), equalTo(TokenRequest(
            grant_type = "authorization_code",
            refresh_token = null,
            client_id = null,
            code = null,
            redirect_uri = null
        )))
    }

    @Test
    fun `deserialize with implicit nulls`() {
        val json = """{"grant_type":"authorization_code"}"""

        assertThat(marshaller.asA(json), equalTo(TokenRequest(
            grant_type = "authorization_code",
            refresh_token = null,
            client_id = null,
            code = null,
            redirect_uri = null
        )))
    }

    @Test
    fun `deserialize full json`() {
        val json = """{
            "grant_type":"authorization_code",
            "refresh_token":"i_am_refreshed",
            "client_id":"special_client",
            "code":"give_me_the_code",
            "redirect_uri":"https://auth.com"
        }"""

        assertThat(marshaller.asA(json), equalTo(TokenRequest(
            grant_type = "authorization_code",
            refresh_token = "i_am_refreshed",
            client_id = "special_client",
            code = "give_me_the_code",
            redirect_uri = Uri.of("https://auth.com")
        )))
    }

    @Test
    fun `serialize full request`(approver: Approver) {
        val data = TokenRequest(
            grant_type = "authorization_code",
            refresh_token = "i_am_refreshed",
            client_id = "special_client",
            code = "give_me_the_code",
            redirect_uri = Uri.of("https://auth.com")
        )

        approver.assertApproved(Response(OK).with(tokenRequestLens of data))
    }

    @Test
    fun `serialize partial request`(approver: Approver) {
        val data = TokenRequest(
            grant_type = "authorization_code",
            refresh_token = null,
            client_id = null,
            code = null,
            redirect_uri = null
        )

        approver.assertApproved(Response(OK).with(tokenRequestLens of data))
    }
}
