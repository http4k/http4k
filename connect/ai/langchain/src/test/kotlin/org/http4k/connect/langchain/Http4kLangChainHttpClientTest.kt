package org.http4k.connect.langchain

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.startsWith
import dev.langchain4j.model.chat.request.ChatRequest
import dev.langchain4j.model.chat.response.ChatResponse
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler
import dev.langchain4j.model.openai.OpenAiChatModel
import dev.langchain4j.model.openai.OpenAiStreamingChatModel
import dev.langchain4j.model.openai.internal.chat.AssistantMessage
import dev.langchain4j.model.openai.internal.chat.ChatCompletionChoice
import dev.langchain4j.model.openai.internal.chat.ChatCompletionResponse
import dev.langchain4j.model.openai.internal.chat.Delta
import org.http4k.connect.openai.FakeOpenAI
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.format.Jackson
import org.http4k.sse.SseMessage
import org.junit.jupiter.api.Test


class Http4kLangChainHttpClientTest {

    @Test
    fun `can call through to model using http4k adapter`() = runBlocking {
        val model = OpenAiChatModel
            .builder()
            .modelName("model")
            .httpClientBuilder(
                FakeOpenAI().asLangchainHttpClientBuilder()
            ).apiKey("foobar")
            .build()

        assertThat(model.chat("hello world"), startsWith("Lorem"))
    }

    @Test
    fun `can call through to streaming model using http4k adapter`() = runBlocking {
        val model = OpenAiStreamingChatModel
            .builder()
            .modelName("model")
            .httpClientBuilder(
                { req: Request ->
                    val content = Jackson.asFormatString(
                        ChatCompletionResponse.builder()
                            .choices(
                                listOf(
                                    ChatCompletionChoice.builder()
                                        .delta(Delta.builder().content("lorem ipsum").build())
                                        .build()
                                )
                            )
                            .build()
                    )

                    Response(OK).body(
                        SseMessage.Event("first", content).toMessage() + SseMessage.Event("[DONE]", "").toMessage()
                    )
                }
                    .asLangchainHttpClientBuilder()
            )
            .apiKey("foobar")
            .build()

        val received = mutableListOf<String>()

        model.chat("hello world", object : StreamingChatResponseHandler {
            override fun onPartialResponse(partialResponse: String) {
                received += partialResponse
            }

            override fun onCompleteResponse(completeResponse: ChatResponse) {
            }

            override fun onError(error: Throwable) {
            }
        })

        assertThat(received.first(), startsWith("lorem ipsum"))
    }
}
