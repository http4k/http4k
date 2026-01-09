package org.http4k.ai.llm.chat

import org.http4k.ai.llm.tools.ToolRequest

sealed class SessionEvent {
    data class UserInput(val message: String) : SessionEvent()
    data class ToolApproved(val toolRequest: ToolRequest) : SessionEvent()
    data class ToolRejected(val toolRequest: ToolRequest) : SessionEvent()
}
