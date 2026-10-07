package org.http4k.connect.openai

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.greaterThan
import com.natpryce.hamkrest.has
import com.natpryce.hamkrest.isA
import com.natpryce.hamkrest.isIn
import com.natpryce.hamkrest.isWithin
import com.natpryce.hamkrest.present
import com.natpryce.hamkrest.startsWith
import org.http4k.ai.model.MaxTokens
import org.http4k.connect.openai.ObjectType.Companion.ChatCompletion
import org.http4k.connect.openai.ObjectType.Companion.ChatCompletionChunk
import org.http4k.connect.openai.OpenAIModels.GPT3_5
import org.http4k.connect.openai.OpenAIModels.GPT_6_LUNA
import org.http4k.connect.openai.OpenAIOrg.Companion.OPENAI
import org.http4k.connect.openai.action.DecisionAnswer
import org.http4k.connect.openai.action.DecisionChoice
import org.http4k.connect.openai.action.DecisionInputMessage
import org.http4k.connect.openai.action.DecisionInputPart
import org.http4k.connect.openai.action.DecisionLevel
import org.http4k.connect.openai.action.DecisionQuestion
import org.http4k.connect.openai.action.ImageResponseFormat
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
            GPT3_5,
            listOf(
                Message.System("You are Leonardo Da Vinci"),
                Message.User("What is your favourite colour?")
            ),
            MaxTokens.of(1000),
            stream = false
        ).successValue().toList()
        assertThat(responses.size, equalTo(1))
        assertThat(responses.first().usage, present())
        assertThat(responses.first().objectType, equalTo(ChatCompletion))
    }

    @Test
    fun `get chat response streaming`() {
        val responses = openAi.chatCompletion(
            GPT3_5,
            listOf(
                Message.System("You are Leonardo Da Vinci"),
                Message.User("What is your favourite colour?")
            ),
            MaxTokens.of(1000),
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
                OpenAIModels.TEXT_EMBEDDING_ADA_002,
                listOf("What is your favourite colour?")
            ).successValue().model.value,
            startsWith("text-embedding-ada-002")
        )
    }

    @Test
    fun `can generate image`(approver: Approver) {
        openAi.generateImage("An excellent library",
            Size.`256x256`,
            ImageResponseFormat.url,
            1,
            null,
            null).successValue()
    }

    @Test
    fun `decides every question it is asked, at the right type`() {
        val decision = openAi.createDecision(
            GPT_6_LUNA,
            "I was charged twice for my order and the export screen is broken.",
            listOf(
                DecisionQuestion.Predicate("damaged", "Does the customer report a damaged item?"),
                DecisionQuestion.Choice(
                    "department", "Which department should handle this complaint?",
                    listOf(DecisionChoice("billing", "Payments, invoices, and refunds."), DecisionChoice("other"))
                ),
                DecisionQuestion.Score(
                    "severity", "How severe is this issue?",
                    listOf(DecisionLevel("Cosmetic"), DecisionLevel("Workaround available"), DecisionLevel("Fully blocked"))
                )
            )
        ).successValue()

        assertThat(decision.model.value, startsWith("gpt-6-luna"))
        assertThat(decision.usage.total_tokens, greaterThan(0))
        assertThat(decision.answers.map { it.name }, equalTo(listOf<String?>("damaged", "department", "severity")))

        assertThat(decision.answers[0], isA<DecisionAnswer.Predicate>(has(DecisionAnswer.Predicate::probability, isWithin(0.0..1.0))))
        assertThat(
            decision.answers[1], isA<DecisionAnswer.Choice>(
                has(DecisionAnswer.Choice::choice, isIn(setOf("billing", "other"))) and
                    has(DecisionAnswer.Choice::probabilities, has(List<DecisionAnswer.ChoiceProbability>::size, equalTo(2)))
            )
        )
        assertThat(
            decision.answers[2], isA<DecisionAnswer.Score>(
                has(DecisionAnswer.Score::score, isWithin(0.0..2.0)) and
                    has(DecisionAnswer.Score::probabilities, has(List<DecisionAnswer.ScoreProbability>::size, equalTo(3)))
            )
        )
    }

    @Test
    fun `decides on image input`() {
        val decision = openAi.createDecision(
            GPT_6_LUNA,
            listOf(
                DecisionInputMessage(
                    listOf(
                        DecisionInputPart.Text("Inspect the product in this photo."),
                        DecisionInputPart.Image(ONE_PIXEL_PNG)
                    )
                )
            ),
            listOf(DecisionQuestion.Predicate("visible_damage", "Does the product have visible damage?"))
        ).successValue()

        assertThat(decision.answers.single(), isA<DecisionAnswer.Predicate>())
    }
}

private const val ONE_PIXEL_PNG =
    "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg=="
