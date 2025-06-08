package chatzilla.endpoints.tools

import chatzilla.ChatHistory
import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.onFailure
import org.http4k.ai.llm.chat.ChatJson.datastarModel
import org.http4k.ai.llm.chat.ChatSessionHandler
import org.http4k.ai.llm.chat.ChatSessionState.AwaitingApproval
import org.http4k.ai.llm.chat.ChatSessionState.Responding
import org.http4k.ai.model.RequestId
import org.http4k.ai.model.ToolName
import org.http4k.datastar.MergeMode.append
import org.http4k.datastar.Selector
import org.http4k.routing.sse
import org.http4k.routing.sse.bind
import org.http4k.sse.sendMergeFragments
import org.http4k.template.DatastarFragmentRenderer

data class ToolApproval(val id: RequestId, val toolName: ToolName)

fun ApproveTool(history: ChatHistory, renderer: DatastarFragmentRenderer, handler: ChatSessionHandler) =
    "/approve" bind sse { sse ->
        val approval = sse.datastarModel<ToolApproval>()
        sse.sendMergeFragments(
            renderer(history.addToolApproved(approval.id)),
            selector = Selector.of("#" + approval.id)
        )

        handler.onToolApprove(approval.toolName)
            .map { newState ->
                when (newState) {
                    is AwaitingApproval -> {
                        sse.sendMergeFragments(
                            renderer(
                                history.addAi(newState.contents),
                                history.addToolConsent(newState.pendingTools.first())
                            ),
                            append,
                            Selector.of("#chat-container")
                        ).close()
                    }

                    is Responding -> {
                        sse.sendMergeFragments(
                            renderer(history.addAi(newState.contents)),
                            append,
                            Selector.of("#chat-container")
                        ).close()
                    }

                    else -> sse.close()
                }
            }.onFailure {
                sse.close()
                // send error to UI
                error(it)
            }
    }
