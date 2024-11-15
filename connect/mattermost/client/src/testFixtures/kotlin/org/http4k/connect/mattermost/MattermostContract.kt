package org.http4k.connect.mattermost

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.Success
import org.http4k.connect.mattermost.action.TriggerWebhookPayload
import org.http4k.core.HttpHandler
import org.http4k.core.Uri
import org.junit.jupiter.api.Test
import java.util.UUID

abstract class MattermostContract {
    abstract val http: HttpHandler
    abstract val uri: Uri

    private val mattermost by lazy {
        Mattermost.Http(uri, http)
    }

    @Test
    fun `can trigger webhook`() {
        assertThat(
            mattermost.triggerWebhook(UUID.randomUUID().toString(), TriggerWebhookPayload(text = "Hello world !")),
            equalTo(Success("ok"))
        )
    }
}
