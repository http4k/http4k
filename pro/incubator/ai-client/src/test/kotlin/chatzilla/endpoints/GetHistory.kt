package chatzilla.endpoints

import chatzilla.ChatHistory
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.datastar.MergeMode
import org.http4k.datastar.Selector
import org.http4k.lens.datastarFragments
import org.http4k.routing.bind
import org.http4k.template.TemplateRenderer

fun GetHistory(history: ChatHistory, renderer: TemplateRenderer) =
    "/history" bind GET to { _: Request ->
        Response(OK).datastarFragments(
            history.content.map(renderer),
            MergeMode.append,
            Selector.of("#chat-container")
        )
    }
