package org.http4k.ai.llm.chat

import org.http4k.ai.llm.chat.ChatSessionEvent.ToolApproved
import org.http4k.ai.llm.chat.ChatSessionEvent.ToolRejected
import org.http4k.ai.llm.chat.ChatSessionEvent.UserInput
import org.http4k.ai.llm.chat.ChatSessionState.AwaitingApproval
import org.http4k.ai.model.ToolName

class ChatSessionHandler(private val stateMachine: ChatSessionStateMachine) {
    fun onUserMessage(message: String) = stateMachine(UserInput(message))

    fun onToolApprove(toolName: ToolName) = when (val currentState = stateMachine.currentState()) {
        is AwaitingApproval -> stateMachine(ToolApproved(currentState.pendingTools.first { it.name == toolName }))
        else -> currentState
    }

    fun onToolReject(toolName: ToolName) = when (val currentState = stateMachine.currentState()) {
        is AwaitingApproval -> stateMachine(ToolRejected(currentState.pendingTools.first { it.name == toolName }))
        else -> currentState
    }
}
