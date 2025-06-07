package chatzilla.endpoints

import chatzilla.ChatHistory
import org.http4k.ai.llm.chat.ChatJson.datastarModel
import org.http4k.ai.llm.chat.ChatSessionHandler
import org.http4k.ai.llm.chat.ChatSessionState.AwaitingApproval
import org.http4k.ai.model.RequestId
import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.datastar.MergeMode.append
import org.http4k.datastar.Selector
import org.http4k.lens.datastarFragments
import org.http4k.routing.bind
import org.http4k.template.DatastarFragmentRenderer

data class ToolDenial(val id: RequestId)

fun DenyTool(history: ChatHistory, renderer: DatastarFragmentRenderer, handler: ChatSessionHandler) =
    "/deny" bind POST to {
        val denial = it.datastarModel<ToolDenial>()
        val response = Response(OK).datastarFragments(
            renderer(history.addToolDenied(denial.id)),
            selector = Selector.of("#" + denial.id)
        )

        when (val newState = handler.onToolApprove()) {
            is AwaitingApproval -> response.datastarFragments(
                renderer(history.addToolConsent(newState.pendingTools.first())),
                append,
                Selector.of("#chat-container")
            )

            else -> response
        }
    }
