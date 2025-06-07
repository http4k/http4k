package chatzilla.endpoints

import chatzilla.ChatHistory
import org.http4k.ai.llm.chat.ChatJson
import org.http4k.ai.llm.chat.ChatJson.datastarModel
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.datastar.MergeMode.append
import org.http4k.datastar.Selector
import org.http4k.format.asDatastarSignal
import org.http4k.lens.datastarFragments
import org.http4k.lens.datastarSignals
import org.http4k.routing.bind
import org.http4k.template.DatastarFragmentRenderer

fun SendUserMessage(history: ChatHistory, renderer: DatastarFragmentRenderer) =
    "/message" bind POST to { req: Request ->
        Response(OK)
            .datastarFragments(
                renderer(
                    history.addUser(req.datastarModel<IncomingMessage>().message),
                    history.addAi("some response")
                ),
                append,
                Selector.of("#chat-container")
            )
            .datastarSignals(ChatJson.asDatastarSignal(IncomingMessage("")))
    }

data class IncomingMessage(val message: String)
