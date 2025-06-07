package org.http4k.ai.llm.chat

import org.http4k.ai.llm.chat.ChatSessionEvent.ToolApproved
import org.http4k.ai.llm.chat.ChatSessionEvent.ToolRejected
import org.http4k.ai.llm.chat.ChatSessionEvent.UserInput
import org.http4k.ai.llm.chat.ChatSessionState.AwaitingApproval
import org.http4k.ai.model.ToolName

class ChatSessionHandler(private val stateMachine: ChatSessionStateMachine) {
    fun onUserMessage(message: String) = stateMachine(UserInput(message))

    fun onToolApprove(toolName: ToolName) = when (val currentState = stateMachine.currentState()) {
        is AwaitingApproval -> {
            val toolRequest = currentState.pendingTools.find { it.name == toolName }
            when(toolRequest) {
                null -> currentState
                else -> stateMachine(ToolApproved(toolRequest))
            }
        }
        else -> currentState
    }

    fun onToolReject(toolName: ToolName) = when (val currentState = stateMachine.currentState()) {
        is AwaitingApproval ->  {
            val toolRequest = currentState.pendingTools.find { it.name == toolName }
            when(toolRequest) {
                null -> currentState
                else -> stateMachine(ToolRejected(toolRequest))
            }
        }
        else -> currentState
    }
}
