package org.http4k.ai.llm.chat

import dev.forkhandles.result4k.Success
import dev.forkhandles.result4k.allValues
import dev.forkhandles.result4k.flatMap
import dev.forkhandles.result4k.map
import org.http4k.ai.llm.LLMResult
import org.http4k.ai.llm.chat.ChatSessionEvent.ToolApproved
import org.http4k.ai.llm.chat.ChatSessionEvent.ToolRejected
import org.http4k.ai.llm.chat.ChatSessionEvent.UserInput
import org.http4k.ai.llm.chat.ChatSessionState.AwaitingApproval
import org.http4k.ai.llm.chat.ChatSessionState.Responding
import org.http4k.ai.llm.chat.ChatSessionState.WaitingForInput
import org.http4k.ai.llm.memory.LLMMemory
import org.http4k.ai.llm.memory.LLMMemoryId
import org.http4k.ai.llm.model.Message
import org.http4k.ai.llm.model.ModelParams
import org.http4k.ai.llm.tools.LLMTool
import org.http4k.ai.llm.tools.LLMTools
import org.http4k.ai.llm.tools.ToolResponse
import org.http4k.ai.model.ToolName

class ChatSessionStateMachine(
    private val llm: Chat,
    private val memory: LLMMemory,
    private val tools: LLMTools,
    private val chatParams: (List<LLMTool>) -> ModelParams
) {
    private var state: ChatSessionState = WaitingForInput
    private lateinit var memoryId: LLMMemoryId

    private val approvedTools = mutableSetOf<ToolName>()

    fun start() = memory.create()
        .map {
            memoryId = it
            state = WaitingForInput
            state
        }

    fun currentState() = state

    operator fun invoke(event: ChatSessionEvent): LLMResult<ChatSessionState> {
        return when (val currentState = state) {
            is WaitingForInput -> when (event) {
                is UserInput -> {
                    memory.add(memoryId, listOf(Message.User(event.message)))
                        .flatMap { processLLMResponse() }
                }

                else -> Success(currentState)
            }

            is AwaitingApproval -> when (event) {
                is ToolApproved -> {
                    val remaining = currentState.pendingTools.filter { it != event.toolRequest }

                    approvedTools.add(event.toolRequest.name)

                    tools(event.toolRequest).map(ToolResponse::result)
                        .flatMap { memory.add(memoryId, listOf(it)) }

                    when {
                        remaining.isEmpty() -> processLLMResponse()
                        else -> Success(AwaitingApproval(emptyList(), remaining))
                    }
                }

                is ToolRejected -> {
                    val remaining = currentState.pendingTools.filter { it != event.toolRequest }
                    when {
                        remaining.isEmpty() -> processLLMResponse()
                        else -> Success(AwaitingApproval(emptyList(), remaining))
                    }
                }

                else -> Success(currentState)
            }

            is Responding -> when (event) {
                is UserInput -> {
                    memory.add(memoryId, listOf(Message.User(event.message)))
                    processLLMResponse()
                }

                else -> Success(WaitingForInput)
            }
        }
            .map {
                state = it
                it
            }
    }

    private fun processLLMResponse() = memory.read(memoryId)
        .flatMap { memory -> tools.list().map { ChatRequest(memory, chatParams(it)) } }
        .flatMap { llm(it) }
        .flatMap { resp -> memory.add(memoryId, listOf(resp.message)).map { resp } }
        .flatMap { it.process() }

    private fun ChatResponse.process(): LLMResult<ChatSessionState> = Success(Unit)
        .flatMap {
            when {
                message.toolRequests.isNotEmpty() -> {
                    val (autoApproved, needApproval) = message.toolRequests
                        .partition { approvedTools.contains(it.name) }

                    autoApproved
                        .map {
                            tools(it).map(ToolResponse::result)
                                .flatMap { memory.add(memoryId, listOf(it)) }
                        }.allValues()
                        .flatMap {
                            when {
                                needApproval.isEmpty() -> processLLMResponse()
                                else -> Success(AwaitingApproval(message.contents, needApproval))
                            }
                        }
                }

                else -> Success(Responding(message.contents))
            }
        }
}
