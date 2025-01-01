package org.http4k.connect.slack.endpoints

import org.http4k.connect.slack.FakeSlackState
import org.http4k.connect.slack.model.ChannelId
import org.http4k.connect.slack.model.SlackMessage
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.routing.bind

fun webhookPostMessage(state: FakeSlackState) =
    "/services/{slack}/{webhook}/{path}" bind Method.POST to { req: Request ->
        val messageRequest = SlackMessage.lens(req)
        state.addMessage(ChannelId.of(req.uri.path.removePrefix("services/")), messageRequest)
        Response(Status.OK)
    }
