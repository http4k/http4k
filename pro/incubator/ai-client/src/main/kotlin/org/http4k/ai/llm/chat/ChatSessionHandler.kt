package org.http4k.ai.llm.chat

import org.http4k.ai.llm.chat.ChatSessionEvent.End
import org.http4k.ai.llm.chat.ChatSessionEvent.ToolApproved
import org.http4k.ai.llm.chat.ChatSessionEvent.ToolRejected
import org.http4k.ai.llm.chat.ChatSessionEvent.UserInput
import org.http4k.ai.llm.chat.ChatSessionState.AwaitingApproval

class ChatSessionHandler(private val stateMachine: ChatSessionStateMachine) {
    fun onUserMessage(message: String) = stateMachine(UserInput(message))

    fun onToolApprove() = when (val currentState = stateMachine.currentState()) {
        is AwaitingApproval -> stateMachine(ToolApproved(currentState.pendingTools.first()))
        else -> currentState
    }

    fun onToolReject() = when (val currentState = stateMachine.currentState()) {
        is AwaitingApproval -> stateMachine(ToolRejected(currentState.pendingTools.first()))
        else -> currentState
    }

    fun onEnd() = stateMachine(End)

    fun currentState() = stateMachine.currentState()
}
