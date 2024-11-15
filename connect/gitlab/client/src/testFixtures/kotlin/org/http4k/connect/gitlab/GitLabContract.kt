package org.http4k.connect.gitlab

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.Result4k
import dev.forkhandles.result4k.Success
import org.http4k.connect.RemoteFailure
import org.http4k.connect.gitlab.api.GitLab
import org.http4k.connect.gitlab.api.GitLabAction
import org.http4k.connect.gitlab.api.Http
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.format.Moshi
import org.junit.jupiter.api.Test

class TestAction : GitLabAction<Map<String, String>> {
    override fun toRequest(): Request = Request(POST, "")
    override fun toResult(response: Response): Result4k<Map<String, String>, RemoteFailure> =
        Success(Moshi.asA<Map<String, String>>(response.bodyString()))
}

class GitLabContract {
    private val gitLab = GitLab.Http(
        { GitLabToken.of("token") }, { Response(OK).body("""{"hello":"world"}""") }
    )

    @Test
    fun `test action`() {
        assertThat(gitLab(TestAction()), equalTo(Success(mapOf("hello" to "world"))))
    }
}
