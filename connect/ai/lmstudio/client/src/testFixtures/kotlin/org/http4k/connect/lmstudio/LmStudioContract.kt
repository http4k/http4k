package org.http4k.connect.lmstudio

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.greaterThan
import com.natpryce.hamkrest.present
import org.http4k.connect.lmstudio.ObjectType.Companion.ChatCompletion
import org.http4k.connect.lmstudio.ObjectType.Companion.ChatCompletionChunk
import org.http4k.connect.lmstudio.action.Message
import org.http4k.connect.model.ModelName
import org.http4k.connect.model.Role
import org.http4k.connect.model.Role.Companion
import org.http4k.connect.model.Role.Companion.System
import org.http4k.connect.successValue
import org.http4k.testing.ApprovalTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ApprovalTest::class)
interface LmStudioContract {

    val lmStudio: LmStudio

    @Test
    fun `get models`() {
        assertThat(
            lmStudio.getModels().successValue().data
                .first().owned_by,
            equalTo(Org.of("organization-owner"))
        )
    }

    @Test
    fun `get chat response non-stream`() {
        val responses = lmStudio.chatCompletion(
            ModelName.CHAT_MODEL,
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
        val responses = lmStudio.chatCompletion(
            ModelName.CHAT_MODEL,
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
            lmStudio.createEmbeddings(
                ModelName.EMBEDDING_MODEL,
                listOf("What is your favourite colour?")
            ).successValue().data.isNotEmpty(),
            equalTo(true)
        )
    }
}
