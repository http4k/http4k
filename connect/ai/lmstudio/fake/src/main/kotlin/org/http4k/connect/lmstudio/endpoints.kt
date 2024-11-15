package org.http4k.connect.lmstudio

import org.http4k.connect.lmstudio.LmStudioMoshi.asFormatString
import org.http4k.connect.lmstudio.LmStudioMoshi.autoBody
import org.http4k.connect.lmstudio.ObjectType.Companion.ChatCompletion
import org.http4k.connect.lmstudio.ObjectType.Companion.ChatCompletionChunk
import org.http4k.connect.lmstudio.action.ChatCompletion
import org.http4k.connect.lmstudio.action.Choice
import org.http4k.connect.lmstudio.action.CompletionResponse
import org.http4k.connect.lmstudio.action.CreateEmbeddings
import org.http4k.connect.lmstudio.action.Embedding
import org.http4k.connect.lmstudio.action.Embeddings
import org.http4k.connect.lmstudio.action.Model
import org.http4k.connect.lmstudio.action.Models
import org.http4k.connect.lmstudio.action.Usage
import org.http4k.connect.model.ModelName
import org.http4k.connect.model.Timestamp
import org.http4k.connect.storage.Storage
import org.http4k.core.ContentType.Companion.TEXT_EVENT_STREAM
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.lens.Header.CONTENT_TYPE
import org.http4k.routing.bind
import java.time.Clock
import java.time.Instant
import java.util.UUID
import kotlin.math.absoluteValue

fun getModels(models: Storage<Model>) = "/v1/models" bind GET to
    {
        Response(OK).with(
            autoBody<Models>().toLens() of
                Models(models.keySet().map { models[it]!! })
        )
    }

fun createEmbeddings(models: Storage<Model>) = "/v1/embeddings" bind POST to
    {
        val request = autoBody<CreateEmbeddings>().toLens()(it)

        models[request.model.value]?.let {
            Response(OK).with(
                autoBody<Embeddings>().toLens() of
                    Embeddings(request.input.mapIndexed { index, token ->
                        Embedding(
                            token.split(" ").map { it.hashCode().absoluteValue / 100000000f }.toFloatArray(),
                            index
                        )
                    }, request.model, Usage(0, 0, 0))

            )
        } ?: Response(NOT_FOUND)
    }

fun chatCompletion(clock: Clock, completionGenerators: Map<ModelName, ChatCompletionGenerator>) =
    "/v1/chat/completions" bind POST to
        { request ->
            val chatRequest = autoBody<ChatCompletion>().toLens()(request)
            val choices = (completionGenerators[chatRequest.model] ?: ChatCompletionGenerator.LoremIpsum())(chatRequest)

            when {
                chatRequest.stream -> {
                    val parts = choices.mapIndexed { it, choice ->
                        asFormatString(
                            completionResponse(
                                request,
                                it,
                                null,
                                ChatCompletionChunk,
                                chatRequest.model,
                                clock.instant(),
                                listOf(choice)
                            )
                        )
                    } + "[DONE]"
                    Response(OK)
                        .with(CONTENT_TYPE of TEXT_EVENT_STREAM.withNoDirectives())
                        .body(parts.joinToString("\n\n") { "data: $it" }.byteInputStream())
                }

                else -> Response(OK).with(
                    autoBody<CompletionResponse>().toLens() of
                        completionResponse(
                            request,
                            0,
                            Usage(0, 0, 0),
                            ChatCompletion,
                            chatRequest.model,
                            clock.instant(),
                            choices
                        )
                )
            }
        }

private fun completionResponse(
    request: Request,
    it: Int,
    usage: Usage?,
    objectType: ObjectType,
    modelName: ModelName,
    now: Instant,
    choices: List<Choice>
): CompletionResponse = CompletionResponse(
    CompletionId.of(
        UUID.nameUUIDFromBytes((request.bodyString() + "$it").toByteArray()).toString()
    ),
    Timestamp.of(now),
    modelName,
    choices,
    objectType,
    usage
)
