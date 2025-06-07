package chatzilla.endpoints

import chatzilla.ChatHistory
import org.http4k.ai.llm.chat.ChatJson.datastarModel
import org.http4k.ai.model.RequestId
import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.datastar.Selector
import org.http4k.lens.datastarFragments
import org.http4k.routing.bind
import org.http4k.template.DatastarFragmentRenderer

data class ToolApproval(val id: RequestId)

fun ApproveTool(history: ChatHistory, renderer: DatastarFragmentRenderer) =
    "/approve" bind POST to {
        val approval = it.datastarModel<ToolApproval>()
        Response(OK).datastarFragments(
            renderer(history.addToolApproved(approval.id)),
            selector = Selector.of("#" + approval.id)
        )
    }
