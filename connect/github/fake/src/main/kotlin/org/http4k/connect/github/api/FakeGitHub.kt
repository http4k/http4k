package org.http4k.connect.github.api

import org.http4k.chaos.ChaoticHttpHandler
import org.http4k.chaos.start
import org.http4k.connect.github.GitHubMoshi
import org.http4k.connect.github.GitHubToken
import org.http4k.connect.github.model.Owner
import org.http4k.connect.storage.InMemory
import org.http4k.connect.storage.Storage
import org.http4k.routing.routes

class FakeGitHub(
    users: Storage<StoredUser> = Storage.InMemory(),
    tokens: Storage<Owner> = Storage.InMemory()
): ChaoticHttpHandler() {

    private val api = FakeGitHubJson(GitHubMoshi, tokens, users)

    override val app = routes(
        api.getAuthedUser(),
        api.getUser(),
        api.getAuthedUserEmails(),
        api.getAuthedUserPublicEmails()
    )

    fun client() = GitHub.Http({ GitHubToken.parse("github_token") }, this)
}

fun main() {
    FakeGitHub().start()
}
