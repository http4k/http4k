package org.http4k.ai.moderation

import org.http4k.ai.AiResult
import org.http4k.ai.model.Message

typealias ModerationModel = (ModerationRequest) -> AiResult<ModerationResponse>

data class ModerationRequest(val messages: List<Message>)

class ModerationResponse(val flagged: Boolean, val flaggedText: String)
