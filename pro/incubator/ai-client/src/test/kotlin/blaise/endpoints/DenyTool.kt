package blaise.endpoints

import blaise.History
import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.onFailure
import org.http4k.ai.llm.chat.ChatJson.datastarModel
import org.http4k.ai.llm.chat.SessionHandler
import org.http4k.ai.llm.chat.SessionState.AwaitingApproval
import org.http4k.ai.llm.chat.SessionState.Responding
import org.http4k.ai.model.RequestId
import org.http4k.ai.model.ToolName
import org.http4k.datastar.MorphMode.append
import org.http4k.datastar.Selector
import org.http4k.routing.sse
import org.http4k.routing.sse.bind
import org.http4k.sse.sendPatchElements
import org.http4k.template.DatastarElementRenderer

data class ToolDenial(val id: RequestId, val toolName: ToolName)

fun DenyTool(history: History, renderer: DatastarElementRenderer, handler: SessionHandler) =
    "/deny" bind sse { sse ->
        val denial = sse.datastarModel<ToolDenial>()
        sse.sendPatchElements(
            renderer(history.addToolDenied(denial.id)),
            selector = Selector.of("#" + denial.id)
        )

        handler.onToolReject(denial.toolName)
            .map { newState ->
                when (newState) {
                    is AwaitingApproval -> sse.sendPatchElements(
                        renderer(
                            history.addAi(newState.contents),
                            history.addToolConsent(newState.pendingTools.first())
                        ),
                        append,
                        Selector.of("#chat-container")
                    ).close()

                    is Responding -> sse.sendPatchElements(
                        renderer(history.addAi(newState.contents)),
                        append,
                        Selector.of("#chat-container")
                    ).close()

                    else -> sse.close()
                }
            }.onFailure {
                sse.close()
                // send error to UI
                error(it)
            }
    }
