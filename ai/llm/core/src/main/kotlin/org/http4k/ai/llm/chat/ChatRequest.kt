package org.http4k.ai.llm.chat

import org.http4k.ai.llm.model.Content.Text
import org.http4k.ai.llm.model.Message
import org.http4k.ai.llm.model.ModelParams

data class ChatRequest(val messages: List<Message>, val params: ModelParams) {
    constructor(message: Message.User, params: ModelParams) : this(listOf(message), params)
    constructor(message: String, params: ModelParams) : this(Message.User(listOf(Text(message))), params)
}
