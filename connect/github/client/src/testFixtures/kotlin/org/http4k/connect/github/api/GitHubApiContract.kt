package org.http4k.connect.github.api

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.connect.RemoteFailure
import org.http4k.connect.failureValue
import org.http4k.connect.github.GitHubToken
import org.http4k.connect.github.model.Owner
import org.http4k.connect.successValue
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Status
import org.http4k.core.Uri
import org.junit.jupiter.api.Test
import java.util.UUID

abstract class GitHubApiContract(private val http: HttpHandler, tokenFn: () -> GitHubToken) {

    protected val gitHub = GitHub.Http(tokenFn, http)

    @Test
    fun `get authorized user`() {
        gitHub.getAuthedUser().successValue()
    }

    @Test
    fun `get user`() {
        val user = gitHub.getAuthedUser().successValue()
        assertThat(gitHub.getUser(user.login).successValue(), equalTo(user))
    }

    @Test
    open fun `get authorized user emails`() {
        gitHub.getAuthedUserEmails().successValue()
    }

    @Test
    fun `get authorized user public emails`() {
        gitHub.getAuthedUserPublicEmails().successValue()
    }

    @Test
    fun `get missing user`() {
        val owner = Owner.parse(UUID.randomUUID().toString())
        gitHub.getUser(owner).failureValue {
            assertThat(it, equalTo(RemoteFailure(
                method = Method.GET,
                uri = Uri.of("/users/$owner"),
                status = Status.NOT_FOUND,
                message = """{"message":"Not Found","documentation_url":"https://docs.github.com/rest","status":"404"}"""
            )))
        }
    }

    @Test
    fun `unauthorized request`() {
        GitHub.Http({ GitHubToken.parse(UUID.randomUUID().toString()) }, http).getAuthedUser().failureValue {
            assertThat(it.status, equalTo(Status(401, "")))
        }
    }
}
