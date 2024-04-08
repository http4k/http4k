package org.http4k.security

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.isA
import dev.forkhandles.result4k.Success
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.format.Jackson
import org.junit.jupiter.api.Test

class ContentTypeJsonOrFormTest {

    @Test
    fun `should extract default access token`() {
        val discordTokenResponse = """
        {
          "token_type": "Bearer",
          "access_token": "ACCESS_TOKEN_REDACTED",
          "expires_in": 604800,
          "refresh_token": "REFRESH_TOKEN_REDACTED",
          "scope": "email guilds.join identify"
        }
        """.trimIndent()
        val result =
            ContentTypeJsonOrForm()(
                Response(OK).header("Content-Type", "application/json").body(discordTokenResponse)
            )
        assertThat(result, isA<Success<AccessTokenResponse>>())
    }

    @Test
    fun `should extract custom access token`() {
        val discordTokenResponse = """
        {
          "token_type": "Bearer",
          "access_token": "ACCESS_TOKEN_REDACTED",
          "expires_in": 604800,
          "refresh_token": "REFRESH_TOKEN_REDACTED",
          "scope": "email guilds.join identify",
          "guild": {
            "id": "1111111111111111111"
          }
        }
        """.trimIndent()
        val result =
            ContentTypeJsonOrForm(Jackson)(
                Response(OK).header("Content-Type", "application/json").body(discordTokenResponse)
            )
        assertThat(result, isA<Success<AccessTokenResponse>>())
    }
}
