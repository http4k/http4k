package org.http4k.connect.slack

import org.http4k.client.JavaHttpClient
import org.http4k.core.HttpHandler
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters.SetBaseUriFrom

fun SlackWebhook.Companion.Http(webhookUrl: Uri, http: HttpHandler = JavaHttpClient()) =
    object : SlackWebhook {
        private val http = SetBaseUriFrom(webhookUrl).then(http)

        override suspend fun <R> invoke(action: SlackWebhookAction<R>) = action.toResult(this.http(action.toRequest()))
    }
