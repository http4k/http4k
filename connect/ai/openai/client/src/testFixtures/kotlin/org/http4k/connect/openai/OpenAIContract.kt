package org.http4k.connect.openai

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.greaterThan
import com.natpryce.hamkrest.present
import com.natpryce.hamkrest.startsWith
import org.http4k.connect.model.ModelName
import org.http4k.connect.model.Role.Companion.System
import org.http4k.connect.model.Role.Companion.User
import org.http4k.connect.openai.ObjectType.Companion.ChatCompletion
import org.http4k.connect.openai.ObjectType.Companion.ChatCompletionChunk
import org.http4k.connect.openai.OpenAIOrg.Companion.OPENAI
import org.http4k.connect.openai.action.Message
import org.http4k.connect.openai.action.Size
import org.http4k.connect.successValue
import org.http4k.testing.ApprovalTest
import org.http4k.testing.Approver
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ApprovalTest::class)
interface OpenAIContract {

    val openAi: OpenAI

    @Test
    fun `get models`() {
        assertThat(
            openAi.getModels().successValue().data
                .first { it.id == ObjectId.of("gpt-4") }.owned_by,
            equalTo(OPENAI)
        )
    }

    @Test
    fun `get chat response non-stream`() {
        val responses = openAi.chatCompletion(
            ModelName.GPT3_5,
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
    fun `get chat response streaming`() {
        val responses = openAi.chatCompletion(
            ModelName.GPT3_5,
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
    fun `get embeddings`() {
        assertThat(
            openAi.createEmbeddings(
                ModelName.TEXT_EMBEDDING_ADA_002,
                listOf("What is your favourite colour?")
            ).successValue().model.value,
            startsWith("text-embedding-ada-002")
        )
    }

    @Test
    fun `can generate image`(approver: Approver) {
        openAi.generateImage("An excellent library", Size.`256x256`).successValue()
    }
}
