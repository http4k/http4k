package org.http4k.connect.azure

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.greaterThan
import com.natpryce.hamkrest.present
import com.natpryce.hamkrest.startsWith
import org.http4k.connect.azure.ObjectType.Companion.ChatCompletion
import org.http4k.connect.azure.ObjectType.Companion.ChatCompletionChunk
import org.http4k.connect.azure.action.Completion
import org.http4k.connect.azure.action.Message
import org.http4k.connect.model.ModelName
import org.http4k.connect.model.Role.Companion.System
import org.http4k.connect.model.Role.Companion.User
import org.http4k.connect.successValue
import org.http4k.testing.ApprovalTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ApprovalTest::class)
interface AzureAIContract {

    val azureAi: AzureAI

    @Test
    fun `get info`() {
        val modelName = ModelName.of("Meta-Llama-3-8B-Instruct")
        val model = azureAi.getInfo(modelName).successValue()
        assertThat(model.model_name, equalTo(modelName))
    }

    @Test
    fun `get chat completion response non-stream`() {
        val responses = azureAi.chatCompletion(
            ModelName.of("Meta-Llama-3-8B-Instruct"),
            listOf(
                Message.System("You are Leonardo Da Vinci"),
                Message.User("What is your favourite colour?")
            ),
            1000,
            stream = false
        ).successValue().toList()
        assertThat(responses.size, equalTo(1))
        assertThat(responses.first().usage, present())
        assertThat(responses.first().objectType, equalTo(ChatCompletion))
    }

    @Test
    fun `get completion response non-stream`() {
        val responses = azureAi(Completion(
            Prompt.of("foobar"),
            stream = false,
            max_tokens = 1000,
        )).successValue().toList()
        assertThat(responses.size, equalTo(1))
        assertThat(responses.first().usage, present())
        assertThat(responses.first().objectType, equalTo(ChatCompletion))
    }

    @Test
    fun `get chat completion response streaming`() {
        val responses = azureAi.chatCompletion(
            ModelName.of("Meta-Llama-3-8B-Instruct"),
            listOf(
                Message.System("You are Leonardo Da Vinci"),
                Message.User("What is your favourite colour?")
            ),
            1000,
            stream = true
        ).successValue().toList()
        assertThat(responses.size, greaterThan(0))
        assertThat(responses.first().usage, absent())
        assertThat(responses.first().objectType, equalTo(ChatCompletionChunk))
    }

    @Test
    fun `get completion response streaming`() {
        val responses = azureAi(
            Completion(
                Prompt.of("what is the best type of cat?"),
                1000,
                stream = true
            )
        ).successValue().toList()
        assertThat(responses.size, greaterThan(0))
        assertThat(responses.first().usage, absent())
        assertThat(responses.first().objectType, equalTo(ChatCompletionChunk))
    }

    @Test
    fun `get embeddings`() {
        assertThat(
            azureAi.createEmbeddings(
                ModelName.of("text-embedding-3-small"),
                listOf("What is your favourite colour?")
            ).successValue().model.value,
            startsWith("text-embedding-3-small")
        )
    }
}
