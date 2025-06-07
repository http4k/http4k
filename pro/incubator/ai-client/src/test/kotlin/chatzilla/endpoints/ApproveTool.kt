package chatzilla.endpoints

import chatzilla.ChatHistory
import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.lens.datastarFragments
import org.http4k.routing.bind
import org.http4k.template.DatastarFragmentRenderer

fun ApproveTool(history: ChatHistory, renderer: DatastarFragmentRenderer) =
    "/approve" bind POST to {
        Response(OK).datastarFragments(renderer(history.addToolApproved("name")))
    }
