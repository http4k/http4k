package chatzilla.endpoints

import chatzilla.ChatHistory
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.datastar.MergeMode.append
import org.http4k.datastar.Selector
import org.http4k.lens.datastarFragments
import org.http4k.routing.bind
import org.http4k.template.TemplateRenderer

fun GetHistory(history: ChatHistory, renderer: TemplateRenderer) =
    "/history" bind GET to {
        Response(OK).datastarFragments(
            history.map(renderer),
            append,
            Selector.of("#chat-container")
        )
    }
