package org.http4k.connect.slack.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class SlackResponse(val ok: Boolean)
