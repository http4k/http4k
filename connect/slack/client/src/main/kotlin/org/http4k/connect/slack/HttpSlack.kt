package org.http4k.connect.slack

import org.http4k.client.JavaHttpClient
import org.http4k.connect.slack.model.SlackToken
import org.http4k.core.HttpHandler
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters.BearerAuth
import org.http4k.filter.ClientFilters.SetBaseUriFrom

fun Slack.Companion.Http(token: () -> SlackToken, http: HttpHandler = JavaHttpClient()) =
    object : Slack {
        private val http = SetBaseUriFrom(Uri.of("https://slack.com")).then(http)

        override fun <R> invoke(action: SlackAction<R>) = action.toResult(
            BearerAuth { token().value }
                .then(this.http)(action.toRequest())
        )
    }


