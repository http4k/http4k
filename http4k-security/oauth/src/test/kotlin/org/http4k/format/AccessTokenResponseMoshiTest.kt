package org.http4k.format

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.with
import org.http4k.security.AccessTokenResponse
import org.http4k.security.accessTokenResponseBody
import org.http4k.testing.Approver
import org.http4k.testing.JsonApprovalTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(JsonApprovalTest::class)
class AccessTokenResponseMoshiTest {

    private val marshaller = OAuthMoshi

    @Test
    fun `deserialize with explicit nulls`() {
        val json = """{
            "access_token":"my_token",
            "token_type":null,
            "expires_in":null,
            "id_token":null,
            "scope":null,
            "refresh_token":null
        }"""

        assertThat(marshaller.asA(json), equalTo(AccessTokenResponse(
            access_token = "my_token",
            token_type = null,
            expires_in = null,
            id_token = null,
            scope = null,
            refresh_token = null
        )))
    }

    @Test
    fun `deserialize with implicit nulls`() {
        val json = """{ "access_token":"my_token"}"""

        assertThat(marshaller.asA(json), equalTo(AccessTokenResponse(
            access_token = "my_token",
            token_type = null,
            expires_in = null,
            id_token = null,
            scope = null,
            refresh_token = null
        )))
    }

    @Test
    fun `deserialize with full json`() {
        val json = """{
            "access_token":"my_token",
            "token_type":"special",
            "expires_in":1337,
            "id_token":"my_id",
            "scope":"full",
            "refresh_token":"i_am_refreshed"
        }"""

        assertThat(marshaller.asA(json), equalTo(AccessTokenResponse(
            access_token = "my_token",
            token_type = "special",
            expires_in = 1337,
            id_token = "my_id",
            scope = "full",
            refresh_token = "i_am_refreshed"
        )))
    }

    @Test
    fun `serialize full response`(approver: Approver) {
        val data = AccessTokenResponse(
            access_token = "my_token",
            token_type = "special",
            expires_in = 1337,
            id_token = "my_id",
            scope = "full",
            refresh_token = "i_am_refreshed"
        )

        approver.assertApproved(Response(Status.OK).with(accessTokenResponseBody of data))
    }

    @Test
    fun `serialize pertial response`(approver: Approver) {
        val data = AccessTokenResponse(
            access_token = "my_token",
            token_type = null,
            expires_in = null,
            id_token = null,
            scope = null,
            refresh_token = null
        )

        approver.assertApproved(Response(Status.OK).with(accessTokenResponseBody of data))
    }
}
