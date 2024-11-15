package org.http4k.connect.ollama

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.greaterThan
import com.natpryce.hamkrest.present
import org.http4k.connect.model.ModelName
import org.http4k.connect.model.Role
import org.http4k.connect.model.Role.Companion.Assistant
import org.http4k.connect.model.Role.Companion.User
import org.http4k.connect.ollama.action.ModelOptions
import org.http4k.connect.successValue
import org.http4k.testing.ApprovalTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ApprovalTest::class)
interface OllamaContract {

    val ollama: Ollama

    @Test
    fun `get models`() {
        assertThat(
            ollama.getModels().successValue().models.isEmpty(),
            equalTo(false)
        )
    }

    @BeforeEach
    fun setup() {
        ollama.pullModel(modelName).successValue()
    }

    val modelName get() = ModelName.of("gemma:2b")

    @Test
    fun `get completion response non-stream`() {
        val responses = ollama.completion(
            modelName,
            Prompt.of("count to five"),
            null,
            false,
            null,
            null,
            null,
            false,
            null,
            ModelOptions(temperature = 0.0)
        ).successValue().toList()
        assertThat(responses.size, equalTo(1))
        assertThat(responses.first().response, present())
    }

    @Test
    fun `get completion response stream`() {
        val responses = ollama.completion(
            modelName,
            Prompt.of("count to five"),
            null,
            true,
            null,
            null,
            null,
            false,
            null,
            ModelOptions(temperature = 0.0)
        ).successValue().toList()
        assertThat(responses.size, greaterThan(1))
        assertThat(responses.first().response, present())
    }

    @Test
    fun `get chat response non-stream`() {
        val responses = ollama.chatCompletion(
            modelName,
            listOf(Message.User("count to five")),
            false,
            null,
            null,
            ModelOptions(temperature = 0.0)
        ).successValue().toList()
        assertThat(responses.size, equalTo(1))
        assertThat(responses.first().message?.role, equalTo(Assistant))
    }

    @Test
    fun `get chat response stream`() {
        val responses = ollama.chatCompletion(
            modelName,
            listOf(Message.User("count to five")),
            true,
            null,
            null,
            ModelOptions(temperature = 0.0)
        ).successValue().toList()
        assertThat(responses.size, greaterThan(1))
        assertThat(responses.first().message?.role, equalTo(Assistant))
    }

    @Test
    fun `get embeddings`() {
        assertThat(
            ollama.createEmbeddings(
                modelName,
                Prompt.of("count to five"),
                null, ModelOptions(temperature = 0.0)
            ).successValue().embedding.size,
            greaterThan(0)
        )
    }
}
