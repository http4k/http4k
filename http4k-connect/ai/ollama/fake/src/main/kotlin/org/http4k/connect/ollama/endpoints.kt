package org.http4k.connect.ollama


import org.http4k.connect.model.ModelName
import org.http4k.connect.model.Role
import org.http4k.connect.model.Role.Companion.Assistant
import org.http4k.connect.model.Role.Companion.User
import org.http4k.connect.ollama.OllamaMoshi.asFormatString
import org.http4k.connect.ollama.OllamaMoshi.autoBody
import org.http4k.connect.ollama.action.ChatCompletion
import org.http4k.connect.ollama.action.ChatCompletionResponse
import org.http4k.connect.ollama.action.Completion
import org.http4k.connect.ollama.action.CompletionResponse
import org.http4k.connect.ollama.action.CreateEmbeddings
import org.http4k.connect.ollama.action.EmbeddingsResponse
import org.http4k.connect.ollama.action.Model
import org.http4k.connect.ollama.action.ModelList
import org.http4k.connect.ollama.action.PullResponse
import org.http4k.connect.storage.Storage
import org.http4k.core.ContentType
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.lens.Header.CONTENT_TYPE
import org.http4k.routing.bind
import java.time.Clock
import java.time.Instant
import kotlin.math.absoluteValue

fun pullModel() = "/api/pull" bind POST to
    {
        Response(OK).with(
            autoBody<PullResponse>().toLens() of
                PullResponse("done")
        )
    }

fun getModels(models: Storage<Model>) = "/api/ps" bind GET to
    {
        Response(OK).with(
            autoBody<ModelList>().toLens() of
                ModelList(models.keySet().map { models[it]!! })
        )
    }

fun createEmbeddings(models: Storage<Model>) = "/api/embeddings" bind POST to
    {
        val request = autoBody<CreateEmbeddings>().toLens()(it)

        models[request.model.value]?.let {
            Response(OK).with(
                autoBody<EmbeddingsResponse>().toLens() of
                    EmbeddingsResponse(
                        request.prompt.value.split(" ").map { token ->
                            token.hashCode().absoluteValue / 100000000f
                        }.toFloatArray(),
                    )
            )
        } ?: Response(NOT_FOUND)
    }

fun chatCompletion(clock: Clock, completionGenerators: Map<ModelName, ChatCompletionGenerator>) =
    "/api/chat" bind POST to
        { request ->
            val completionRequest = autoBody<ChatCompletion>().toLens()(request)
            val choices = (completionGenerators[completionRequest.model] ?: ChatCompletionGenerator.LoremIpsum())
                .invoke(completionRequest.messages, completionRequest.format)

            val parts = choices.map {
                chatCompletionResponse(
                    completionRequest.model,
                    Message(Assistant, it, null),
                    clock.instant(),
                    false
                )
            }.plus(
                chatCompletionResponse(
                    completionRequest.model,
                    Message(Assistant, choices.last(), null),
                    clock.instant(),
                    true
                )
            )

            when {
                completionRequest.stream ?: true -> {
                    Response(OK)
                        .with(CONTENT_TYPE of ContentType("application/x-ndjson").withNoDirectives())
                        .body(parts.joinToString("\n") { asFormatString(it) }.byteInputStream())
                }

                else -> Response(OK).with(
                    autoBody<ChatCompletionResponse>().toLens() of
                        chatCompletionResponse(
                            completionRequest.model,
                            Message(Assistant, parts.last().message?.content ?: "", null),
                            clock.instant(),
                            true
                        )

                )
            }
        }

fun completion(clock: Clock, completionGenerators: Map<ModelName, ChatCompletionGenerator>) =
    "/api/generate" bind POST to
        { request ->
            val completionRequest = autoBody<Completion>().toLens()(request)
            val choices = (completionGenerators[completionRequest.model] ?: ChatCompletionGenerator.LoremIpsum())
                .invoke(listOf(Message(User, completionRequest.prompt.value, null)), completionRequest.format)

            val parts = choices.map {
                completionResponse(completionRequest.model, it, clock.instant(), false)
            }.plus(completionResponse(completionRequest.model, "", clock.instant(), true))
                .map { asFormatString(it) }

            when {
                completionRequest.stream ?: true -> {
                    Response(OK)
                        .with(CONTENT_TYPE of ContentType("application/x-ndjson").withNoDirectives())
                        .body(parts.joinToString("\n").byteInputStream())
                }

                else -> Response(OK).with(
                    autoBody<CompletionResponse>().toLens() of
                        completionResponse(completionRequest.model, parts.last(), clock.instant(), true)
                )
            }
        }

private fun chatCompletionResponse(modelName: ModelName, message: Message?, now: Instant, done: Boolean) =
    ChatCompletionResponse(
        modelName, now, message, done, 0, 0, 0, 0, 0, 0
    )

private fun completionResponse(modelName: ModelName, completion: String?, now: Instant, done: Boolean) =
    CompletionResponse(
        modelName, now, completion, done, listOf(1), 0, 0, 0, 0, 0, 0
    )
