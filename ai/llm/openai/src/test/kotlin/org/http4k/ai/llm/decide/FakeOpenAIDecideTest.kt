package org.http4k.ai.llm.decide

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.isA
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.orThrow
import org.http4k.ai.llm.model.Content
import org.http4k.ai.llm.model.Resource
import org.http4k.ai.model.ApiKey
import org.http4k.connect.model.Base64Blob
import org.http4k.connect.model.MimeType
import org.http4k.connect.openai.FakeOpenAI
import org.http4k.connect.openai.FirstOptionAnswerer
import org.http4k.connect.openai.OpenAIModels
import org.http4k.connect.openai.action.DecisionAnswer
import org.http4k.connect.openai.action.DecisionInputPart
import org.http4k.connect.openai.action.ImageDetail
import org.http4k.core.Uri
import org.junit.jupiter.api.Test

class FakeOpenAIDecideTest : DecideContract {
    override val decide = Decide.OpenAI(ApiKey.of("fake"), FakeOpenAI())
    override val model = OpenAIModels.GPT_6_LUNA

    @Test
    fun `a refused question is answered with a refusal`() {
        val refusing = Decide.OpenAI(ApiKey.of("fake"), FakeOpenAI { _, question -> DecisionAnswer.Refusal(question.name) })

        val response = refusing(
            DecideRequest("Diagnose my rash", mapOf("diagnosis" to Question.YesNo("Is this eczema?")), model)
        ).orThrow { error(it) }

        assertThat(response.answers, equalTo(mapOf<String, Answer>("diagnosis" to Answer.Refusal)))
    }

    @Test
    fun `a binary image reaches OpenAI as a data url`() {
        val received = mutableListOf<DecisionInputPart>()
        val decide = Decide.OpenAI(ApiKey.of("fake"), FakeOpenAI { input, question ->
            received += input.flatMap { it.content }
            FirstOptionAnswerer(input, question)
        })

        decide(
            DecideRequest(
                listOf(
                    Content.Text("Inspect the product in this photo."),
                    Content.Image(Resource.Binary(Base64Blob.of("iVBORw0KGgo="), MimeType.of("image/png")))
                ),
                mapOf("visible_damage" to Question.YesNo("Does the product have visible damage?")),
                model
            )
        ).orThrow { error(it) }

        assertThat(
            received.toList(), equalTo(
                listOf(
                    DecisionInputPart.Text("Inspect the product in this photo."),
                    DecisionInputPart.Image("data:image/png;base64,iVBORw0KGgo=", ImageDetail.low)
                )
            )
        )
    }

    @Test
    fun `OpenAI only decides on text and binary images`() {
        val result = decide(
            DecideRequest(
                listOf(Content.Image(Resource.Ref(Uri.of("https://example.com/photo.png"), MimeType.of("image/png")))),
                mapOf("visible_damage" to Question.YesNo("Does the product have visible damage?")),
                model
            )
        )

        assertThat(result, isA<Failure<*>>())
    }
}
