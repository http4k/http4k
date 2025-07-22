package org.http4k.connect.openai

import org.http4k.ai.model.ModelName
import org.http4k.connect.model.Base64Blob
import org.http4k.connect.model.Timestamp
import org.http4k.connect.openai.ObjectType.Companion.ChatCompletion
import org.http4k.connect.openai.ObjectType.Companion.ChatCompletionChunk
import org.http4k.connect.openai.OpenAIMoshi.asFormatString
import org.http4k.connect.openai.OpenAIMoshi.autoBody
import org.http4k.connect.openai.action.ChatCompletion
import org.http4k.connect.openai.action.Choice
import org.http4k.connect.openai.action.CompletionResponse
import org.http4k.connect.openai.action.CreateEmbeddings
import org.http4k.connect.openai.action.Embedding
import org.http4k.connect.openai.action.Embeddings
import org.http4k.connect.openai.action.GenerateImage
import org.http4k.connect.openai.action.GeneratedImage
import org.http4k.connect.openai.action.ImageData
import org.http4k.connect.openai.action.ImageResponseFormat.b64_json
import org.http4k.connect.openai.action.ImageResponseFormat.url
import org.http4k.connect.openai.action.Model
import org.http4k.connect.openai.action.Models
import org.http4k.connect.openai.action.Usage
import org.http4k.connect.storage.Storage
import org.http4k.core.ContentType.Companion.TEXT_EVENT_STREAM
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.extend
import org.http4k.core.with
import org.http4k.format.MoshiArray
import org.http4k.format.MoshiObject
import org.http4k.format.MoshiString
import org.http4k.lens.Header.CONTENT_TYPE
import org.http4k.routing.ResourceLoader.Companion.Classpath
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.routing.static
import java.time.Clock
import java.time.Instant
import java.util.UUID
import kotlin.math.absoluteValue

fun openAIEndpoints(clock: Clock, baseUri: Uri, models: Storage<Model>, completionGenerators: Map<ModelName, ChatCompletionGenerator>) = routes(
    getModels(models),
    chatCompletion(clock, completionGenerators),
    createEmbeddings(models),
    generateImage(clock, baseUri),
)

fun generateImage(clock: Clock, baseUri: Uri) = "/images/generations" bind POST to
    {
        val request = autoBody<GenerateImage>().toLens()(it)

        val logo = request.size.name + ".png"

        Response(OK).with(
            autoBody<GeneratedImage>().toLens() of GeneratedImage(
                Timestamp.of(clock.instant()),
                listOf(
                    when (request.response_format) {
                        url -> ImageData(url = baseUri.extend(Uri.of("/$logo")))
                        b64_json -> ImageData(b64_json = Base64Blob.encode(FakeOpenAI::class.java.getResourceAsStream("/public/$logo")!!))
                    }
                )
            )
        )
    }

fun getModels(models: Storage<Model>) = "/models" bind GET to
    {
        Response(OK).with(
            autoBody<Models>().toLens() of
                Models(models.keySet().map { models[it]!! })
        )
    }

fun createEmbeddings(models: Storage<Model>) = "/embeddings" bind POST to
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
    "/chat/completions" bind POST to
        { request ->
            val chatRequest = autoBody<ChatCompletion>().toLens()(request.convertSimpleMessageToArrayOfMessages())
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

private fun Request.convertSimpleMessageToArrayOfMessages(): Request {
    val node = OpenAIMoshi.parse(bodyString()) as MoshiObject
    val messages = node["messages"] as MoshiArray

    val firstMessage = messages[0] as MoshiObject
    val content = firstMessage["content"]
    return when {
        content is MoshiString -> {
            node.attributes["messages"] = MoshiArray(
                listOf(
                    MoshiObject(
                        mutableMapOf(
                            "role" to MoshiString("user"),
                            "content" to MoshiArray(
                                MoshiObject(
                                    "type" to MoshiString("text"),
                                    "text" to content
                                )
                            )
                        )
                    )
                ) + messages.elements.drop(1)
            )
            body(asFormatString(node))
        }

        else -> this
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
    UUID.nameUUIDFromBytes((request.bodyString() + "$it").toByteArray()).toString(),
    Timestamp.of(now),
    modelName.value,
    choices,
    objectType.value,
    usage
)

fun serveGeneratedContent() = static(Classpath("public"))
