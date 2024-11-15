package org.http4k.connect.github.webhook

import org.http4k.connect.github.GitHubToken
import org.http4k.connect.github.api.GitHubWebhookAction
import org.http4k.core.HttpHandler
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.filter.ClientFilters.SetBaseUriFrom
import org.http4k.filter.SignGitHubWebhookSha256

fun GitHubWebhook.Companion.Http(url: Uri, token: () -> GitHubToken, http: HttpHandler) = object : GitHubWebhook {
    private val signedHttp = SetBaseUriFrom(url)
        .then(ClientFilters.SignGitHubWebhookSha256(token))
        .then(http)

    override fun invoke(action: GitHubWebhookAction) = action.toResult(signedHttp(action.toRequest()))
}
