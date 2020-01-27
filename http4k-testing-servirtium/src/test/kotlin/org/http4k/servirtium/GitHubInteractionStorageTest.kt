package org.http4k.servirtium

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.throws
import org.http4k.cloudnative.Unauthorized
import org.http4k.core.Credentials
import org.http4k.core.Uri
import org.junit.jupiter.api.Test

class GitHubInteractionStorageTest {
    @Test
    fun `can load content from github`() {
        val storage = gitHubFor(FakeGitHub.credentials)
        println(storage("file.md").get())
    }

    @Test
    fun `wrong creds throws up`() {
        val storage = gitHubFor(Credentials("notuser", "noway"))
        assertThat({ storage("file.md").get() }, throws<Unauthorized>())
    }

    private fun gitHubFor(credentials: Credentials) = InteractionStorage.Github(
        "owner",
        "repo",
        "master",
        credentials,
        Uri.of("https://api.github.com"),
        FakeGitHub()
    )
}
