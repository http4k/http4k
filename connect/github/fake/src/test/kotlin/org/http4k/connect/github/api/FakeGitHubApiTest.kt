package org.http4k.connect.github.api

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.connect.github.GitHubToken
import org.http4k.connect.github.model.AccountType
import org.http4k.connect.github.model.Email
import org.http4k.connect.github.model.Owner
import org.http4k.connect.storage.InMemory
import org.http4k.connect.storage.Storage
import org.http4k.connect.storage.set
import org.http4k.connect.successValue
import org.http4k.filter.debug
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant

private val myToken = GitHubToken.parse("fake")
private val myOwner = Owner.parse("catsquid")
private val t0 = Instant.parse("2026-01-01T12:00:00Z")

private val email1 = Email(
    email = "squid@co.com",
    verified = false,
    primary = false,
    visibility = "public"
)

private val email2 = Email(
    email = "squid@gmail.com",
    verified = true,
    primary = false,
    visibility = "public"
)

private val email3 = Email(
    email = "squid@proton.me",
    verified = true,
    primary = false,
    visibility = "private"
)

class FakeGitHubApiTest : GitHubApiContract(
    http = run {
        val users = Storage.InMemory<StoredUser>()
        val tokens = Storage.InMemory<Owner>()

        tokens[myToken.value] = myOwner
        users[myOwner] = StoredUser(
            login = myOwner,
            name = "The Cat Squid",
            company = "Squid Co",
            type = AccountType.User,
            createdAt = t0,
            updatedAt = t0 + Duration.ofDays(30),
            emails = listOf(email1, email2, email3)
        )

        FakeGitHub(users, tokens).debug()
    },
    tokenFn = { myToken }
) {
    @Test
    fun `get and verify authorized user emails`() {
        gitHub.getAuthedUserEmails().successValue {
            assertThat(it.toSet(), equalTo(setOf(email1, email2, email3)))
        }
    }

    @Test
    fun `get and verify authorized user public emails`() {
        gitHub.getAuthedUserPublicEmails().successValue {
            assertThat(it.toSet(), equalTo(setOf(email1, email2)))
        }
    }
}
