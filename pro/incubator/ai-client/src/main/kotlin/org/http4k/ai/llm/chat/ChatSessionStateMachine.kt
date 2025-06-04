package org.http4k.ai.llm.chat

import dev.forkhandles.result4k.flatMap
import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.valueOrNull
import org.http4k.ai.llm.chat.ChatSessionEvent.End
import org.http4k.ai.llm.chat.ChatSessionEvent.ProcessResponse
import org.http4k.ai.llm.chat.ChatSessionEvent.ToolApproved
import org.http4k.ai.llm.chat.ChatSessionEvent.ToolRejected
import org.http4k.ai.llm.chat.ChatSessionEvent.UserInput
import org.http4k.ai.llm.chat.ChatSessionState.AwaitingApproval
import org.http4k.ai.llm.chat.ChatSessionState.Finished
import org.http4k.ai.llm.chat.ChatSessionState.Processing
import org.http4k.ai.llm.chat.ChatSessionState.Responding
import org.http4k.ai.llm.chat.ChatSessionState.ToolInvocation
import org.http4k.ai.llm.chat.ChatSessionState.WaitingForInput
import org.http4k.ai.llm.memory.LLMMemory
import org.http4k.ai.llm.memory.LLMMemoryId
import org.http4k.ai.llm.model.Message
import org.http4k.ai.llm.model.ModelParams
import org.http4k.ai.llm.tools.LLMTool
import org.http4k.ai.llm.tools.ToolResponse
import org.http4k.ai.llm.tools.Tools
import org.http4k.ai.model.ToolName

class ChatSessionStateMachine(
    private val llm: Chat,
    private val memory: LLMMemory,
    private val tools: Tools,
    private val chatParams: (List<LLMTool>) -> ModelParams
) {
    private var state: ChatSessionState = WaitingForInput
    private lateinit var memoryId: LLMMemoryId

    private val approvedTools = mutableSetOf<ToolName>()

    fun start(): ChatSessionStateMachine {
        memoryId = memory.create().valueOrNull()!!
        state = WaitingForInput
        return this
    }

    fun currentState() = state

    operator fun invoke(event: ChatSessionEvent): ChatSessionState {
        state = when (val currentState = state) {
            is WaitingForInput -> when (event) {
                is UserInput -> {
                    memory.add(memoryId, listOf(Message.User(event.message)))
                    processLLMResponse()
                }

                else -> currentState
            }

            is Processing -> when (event) {
                is ProcessResponse -> event.response.process()
                else -> currentState
            }

            is AwaitingApproval -> when (event) {
                is ToolApproved -> {
                    val remaining = currentState.pendingTools.filter { it != event.toolRequest }

                    approvedTools.add(event.toolRequest.name)

                    tools(event.toolRequest).map(ToolResponse::result)
                        .flatMap { memory.add(memoryId, listOf(it)) }
                        .valueOrNull()!!

                    when {
                        remaining.isEmpty() -> processLLMResponse()
                        else -> AwaitingApproval(remaining)
                    }
                }

                is ToolRejected -> {
                    val remaining = currentState.pendingTools.filter { it != event.toolRequest }
                    when {
                        remaining.isEmpty() -> processLLMResponse()
                        else -> AwaitingApproval(remaining)
                    }
                }

                else -> currentState
            }

            is ToolInvocation -> currentState

            is Responding -> when (event) {
                is UserInput -> {
                    memory.add(memoryId, listOf(Message.User(event.message)))
                    processLLMResponse()
                }

                is End -> Finished
                else -> WaitingForInput
            }

            is Finished -> currentState
        }

        return state
    }

    private fun processLLMResponse(): ChatSessionState = llm(
        ChatRequest(
            memory.read(memoryId).valueOrNull()!!,
            chatParams(tools.list().valueOrNull()!!)
        )
    )
        .flatMap { resp -> memory.add(memoryId, listOf(resp.message)).map { resp } }
        .map { it.process() }
        .valueOrNull()!!

    private fun ChatResponse.process() = when {
        message.toolRequests.isNotEmpty() -> {
            val (autoApproved, needApproval) = message.toolRequests.partition {
                approvedTools.contains(it.name)
            }

            autoApproved.forEach {
                tools(it).map(ToolResponse::result)
                    .flatMap { memory.add(memoryId, listOf(it)) }
                    .valueOrNull()
            }

            when {
                needApproval.isNotEmpty() -> AwaitingApproval(needApproval)
                else -> processLLMResponse()
            }
        }

        else -> Responding(this)
    }
}
