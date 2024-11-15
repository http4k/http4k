package org.http4k.connect.github

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.Success
import org.http4k.connect.github.api.GitHubWebhookAction
import org.http4k.connect.github.webhook.GitHubWebhook
import org.http4k.connect.github.webhook.Http
import org.http4k.connect.github.webhook.WebhookEventType
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ServerFilters
import org.http4k.filter.VerifyGitHubSignatureSha256
import org.junit.jupiter.api.Test

class TestWebhookAction : GitHubWebhookAction(WebhookEventType.check_suite)

class GitHubWebhookContract {
    private val secret = { GitHubToken.of("secret") }

    private val server = ServerFilters.VerifyGitHubSignatureSha256(secret)
        .then {
            Response(OK)
        }

    private val webhook = GitHubWebhook.Http(Uri.of("/foobar"), secret, server)

    @Test
    fun `test webhook`() {
        assertThat(webhook(TestWebhookAction()), equalTo(Success(Unit)))
    }
}
