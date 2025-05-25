package org.http4k.connect.langchain

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.startsWith
import dev.langchain4j.model.openai.OpenAiChatModel
import org.http4k.connect.openai.FakeOpenAI
import org.http4k.filter.debug
import org.junit.jupiter.api.Test

class Http4kLangChainHttpClientTest {

    @Test
    fun `can call through to model using http4k adapter`() {
        val model = OpenAiChatModel
            .builder()
            .modelName("model")
            .httpClientBuilder(
                FakeOpenAI().asLangchainHttpClientBuilder()
            ).apiKey("foobar")
            .build()

        assertThat(model.chat("hello world"), startsWith("Lorem"))
    }
}
