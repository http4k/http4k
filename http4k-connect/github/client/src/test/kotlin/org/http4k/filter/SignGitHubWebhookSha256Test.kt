package org.http4k.filter

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.connect.github.GitHubToken
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.I_M_A_TEAPOT
import org.http4k.core.then
import org.http4k.hamkrest.hasStatus
import org.http4k.lens.Header
import org.http4k.lens.X_HUB_SIGNATURE_256
import org.junit.jupiter.api.Test

class SignGitHubWebhookSha256Test {

    private val app = ClientFilters.SignGitHubWebhookSha256 { GitHubToken.of("secret") }
        .then {
            assertThat(
                Header.X_HUB_SIGNATURE_256(it),
                equalTo("734cc62f32841568f45715aeb9f4d7891324e6d948e4c6c60c0621cdac48623a")
            )
            Response(I_M_A_TEAPOT)
        }

    @Test
    fun `signs correctly`() {
        assertThat(
            app(
                Request(POST, "")
                    .body("hello world")
            ), hasStatus(I_M_A_TEAPOT)
        )
    }
}
