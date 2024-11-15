package org.http4k.connect.anthropic.endpoints

import org.http4k.connect.anthropic.AnthropicAIMoshi
import org.http4k.connect.anthropic.AnthropicAIMoshi.autoBody
import org.http4k.connect.anthropic.LoremIpsum
import org.http4k.connect.anthropic.MessageContentGenerator
import org.http4k.connect.anthropic.ResponseId
import org.http4k.connect.anthropic.StopReason
import org.http4k.connect.anthropic.action.MessageCompletion
import org.http4k.connect.anthropic.action.MessageCompletionResponse
import org.http4k.connect.anthropic.action.MessageCompletionStream
import org.http4k.connect.anthropic.action.MessageGenerationEvent
import org.http4k.connect.anthropic.action.Usage
import org.http4k.connect.model.ModelName
import org.http4k.connect.model.Role
import org.http4k.core.ContentType.Companion.TEXT_EVENT_STREAM
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.lens.Header.CONTENT_TYPE
import org.http4k.routing.bind

fun messageCompletion(completionGenerators: Map<ModelName, MessageContentGenerator>) =
    "/v1/messages" bind Method.POST to
        { request ->
            val messageRequest = runCatching {
                autoBody<MessageCompletion>().toLens()(request)
            }.mapCatching {
                autoBody<MessageCompletionStream>().toLens()(request)
            }.getOrThrow()

            val choices =
                (completionGenerators[messageRequest.model]
                    ?: MessageContentGenerator.LoremIpsum())(messageRequest.messages)

            when {
                messageRequest.stream -> {
                    val parts = listOf(
                        "data: " +
                            AnthropicAIMoshi.asFormatString(
                                MessageGenerationEvent.StartMessage(
                                    MessageCompletionResponse(
                                        ResponseId.of(messageRequest.hashCode().toString()),
                                        Role.Assistant,
                                        choices,
                                        messageRequest.model,
                                        StopReason.end_turn,
                                        null,
                                        Usage(1, 1, 1, 1)
                                    )
                                ),
                            )
                    ) + "event: message_stop"

                    Response(OK)
                        .with(CONTENT_TYPE of TEXT_EVENT_STREAM.withNoDirectives())
                        .body(parts.joinToString("\n\n").byteInputStream())
                }

                else -> Response(OK).with(
                    autoBody<MessageCompletionResponse>().toLens() of
                        MessageCompletionResponse(
                            ResponseId.of(messageRequest.hashCode().toString()),
                            Role.Assistant,
                            choices,
                            messageRequest.model,
                            StopReason.end_turn,
                            null,
                            Usage(1, 1, 1, 1)
                        )
                )
            }
        }
