package org.http4k.connect.anthropic

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.greaterThan
import com.natpryce.hamkrest.isA
import org.http4k.connect.ResourceLoader
import org.http4k.connect.TestResources
import org.http4k.connect.anthropic.action.Content
import org.http4k.connect.anthropic.action.Message
import org.http4k.connect.anthropic.action.MessageGenerationEvent
import org.http4k.connect.anthropic.action.Source
import org.http4k.connect.model.Base64Blob
import org.http4k.connect.model.ModelName
import org.http4k.connect.successValue
import org.http4k.testing.ApprovalTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ApprovalTest::class)
@ExtendWith(TestResources::class)
interface AnthropicAIContract {

    val anthropicAi: AnthropicAI

    @Test
    fun `generate message response non-stream`(resourceLoader: ResourceLoader) {
        val responses = anthropicAi.messageCompletion(
            ModelName.of("claude-3-5-sonnet-20240620"),
            listOf(
                Message.User(
                    listOf(
                        Content.Image(
                            Source(
                                Base64Blob.encode(resourceLoader.stream("dog.png")),
                                MediaType.IMAGE_PNG
                            )
                        ),
                        Content.Text("What is in the image?"),
                    )
                )
            ),
            100,
        ).successValue()

        assertThat(responses.usage.input_tokens!!, greaterThan(0))
    }

    @Test
    fun `generate message response stream`() {
        val responses = anthropicAi.messageCompletionStream(
            ModelName.of("claude-3-5-sonnet-20240620"),
            listOf(
                Message.User(listOf(Content.Text("You are Leonardo Da Vinci")))
            ),
            100,
        ).successValue().toList()

        assertThat(responses.first(), isA<MessageGenerationEvent.StartMessage>())
    }
}
