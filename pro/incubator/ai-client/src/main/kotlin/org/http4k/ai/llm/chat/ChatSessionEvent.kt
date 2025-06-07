package org.http4k.ai.llm.chat

import org.http4k.ai.llm.tools.ToolRequest

sealed class ChatSessionEvent {
    data class UserInput(val message: String) : ChatSessionEvent()
    data class ToolApproved(val toolRequest: ToolRequest) : ChatSessionEvent()
    data class ToolRejected(val toolRequest: ToolRequest) : ChatSessionEvent()
}
