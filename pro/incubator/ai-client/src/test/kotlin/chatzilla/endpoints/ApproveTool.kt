package chatzilla.endpoints

import chatzilla.ChatHistory
import org.http4k.ai.llm.chat.ChatJson.datastarModel
import org.http4k.ai.llm.chat.ChatSessionHandler
import org.http4k.ai.llm.chat.ChatSessionState.AwaitingApproval
import org.http4k.ai.llm.chat.ChatSessionState.Processing
import org.http4k.ai.llm.chat.ChatSessionState.Responding
import org.http4k.ai.model.RequestId
import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.datastar.MergeMode.append
import org.http4k.datastar.Selector
import org.http4k.lens.datastarFragments
import org.http4k.routing.bind
import org.http4k.template.DatastarFragmentRenderer

data class ToolApproval(val id: RequestId)

fun ApproveTool(history: ChatHistory, renderer: DatastarFragmentRenderer, handler: ChatSessionHandler) =
    "/approve" bind POST to {
        val approval = it.datastarModel<ToolApproval>()
        println(approval)
        val response = Response(OK).datastarFragments(
            renderer(history.addToolApproved(approval.id)),
            selector = Selector.of("#" + approval.id)
        )

        when (val newState = handler.onToolApprove()) {
            is AwaitingApproval -> response.datastarFragments(
                renderer(history.addToolConsent(newState.pendingTools.first())),
                append,
                Selector.of("#chat-container")
            )

            is Processing -> response.datastarFragments(
                renderer(history.addAi(newState.message)),
                append,
                Selector.of("#chat-container")
            )

            is Responding -> response.datastarFragments(
                renderer(history.addAi(newState.response.message.text ?: "")),
                append,
                Selector.of("#chat-container")
            )

            else -> response
        }
    }
