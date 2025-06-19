package org.http4k.ai.llm.chat

import dev.forkhandles.result4k.Success
import org.http4k.ai.llm.chat.ChatSessionEvent.ToolApproved
import org.http4k.ai.llm.chat.ChatSessionEvent.ToolRejected
import org.http4k.ai.llm.chat.ChatSessionEvent.UserInput
import org.http4k.ai.llm.chat.ChatSessionState.AwaitingApproval
import org.http4k.ai.model.ToolName

class ChatSessionHandler(private val stateMachine: ChatSessionStateMachine) {
    fun onUserMessage(message: String) = stateMachine(UserInput(message))

    fun onToolApprove(toolName: ToolName) = when (val currentState = stateMachine.currentState()) {
        is AwaitingApproval -> currentState.pendingTools
            .find { it.name == toolName }
            ?.let { stateMachine(ToolApproved(it)) } ?: Success(currentState)

        else -> Success(currentState)
    }

    fun onToolReject(toolName: ToolName) = when (val currentState = stateMachine.currentState()) {
        is AwaitingApproval -> currentState.pendingTools
            .find { it.name == toolName }
            ?.let { stateMachine(ToolRejected(it)) } ?: Success(currentState)

        else -> Success(currentState)
    }
}
