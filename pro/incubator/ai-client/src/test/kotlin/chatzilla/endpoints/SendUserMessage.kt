package chatzilla.endpoints

import chatzilla.ChatHistory
import dev.forkhandles.result4k.flatMap
import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.onFailure
import org.http4k.ai.llm.chat.ChatJson
import org.http4k.ai.llm.chat.ChatJson.datastarModel
import org.http4k.ai.llm.chat.ChatSessionHandler
import org.http4k.ai.llm.chat.ChatSessionState.AwaitingApproval
import org.http4k.ai.llm.chat.ChatSessionState.Responding
import org.http4k.core.Method.POST
import org.http4k.datastar.MorphMode.append
import org.http4k.datastar.Selector
import org.http4k.format.asDatastarSignal
import org.http4k.routing.sse
import org.http4k.routing.sse.bind
import org.http4k.sse.sendPatchElements
import org.http4k.sse.sendPatchSignals
import org.http4k.template.DatastarElementRenderer

fun SendUserMessage(history: ChatHistory, renderer: DatastarElementRenderer, handler: ChatSessionHandler) =
    "/message" bind sse(
        POST to sse { sse ->
            val message = sse.datastarModel<IncomingMessage>().message

            sse
                .sendPatchElements(renderer(history.addUser(message)), append, Selector.of("#chat-container"))
                .sendPatchSignals(ChatJson.asDatastarSignal(IncomingMessage("")))

            handler.onUserMessage(message)
                .flatMap { handler.onUserMessage(message) }
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
    )

data class IncomingMessage(val message: String)
