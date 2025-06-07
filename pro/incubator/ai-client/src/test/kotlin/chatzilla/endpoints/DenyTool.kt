package chatzilla.endpoints

import chatzilla.ChatHistory
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.lens.datastarFragments
import org.http4k.routing.bind
import org.http4k.template.DatastarFragmentRenderer

fun DenyTool(history: ChatHistory, renderer: DatastarFragmentRenderer) =
    "/deny" bind Method.POST to {
        Response(Status.OK).datastarFragments(renderer(history.addToolDenied("name")))
    }
