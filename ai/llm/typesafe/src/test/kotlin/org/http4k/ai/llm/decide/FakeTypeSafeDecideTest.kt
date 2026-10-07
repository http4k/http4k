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
import org.http4k.connect.typesafe.FakeTypeSafe
import org.http4k.connect.typesafe.FirstCriterionAnswerer
import org.http4k.connect.typesafe.JevModels
import org.http4k.connect.typesafe.Question as TypeSafeQuestion
import org.http4k.format.unwrap
import org.junit.jupiter.api.Test

class FakeTypeSafeDecideTest : DecideContract {
    override val decide = Decide.TypeSafe(ApiKey.of("fake"), FakeTypeSafe())
    override val model = JevModels.JevLatest

    @Test
    fun `score levels reach TypeSafe as criteria combining label and description`() {
        val criteria = mutableListOf<String?>()
        val decide = Decide.TypeSafe(ApiKey.of("fake"), FakeTypeSafe { state, id, question ->
            criteria += (question as TypeSafeQuestion.Score).criteria.map { it.unwrap()?.toString() }
            FirstCriterionAnswerer(state, id, question)
        })

        decide(
            DecideRequest(
                "The export button does nothing",
                mapOf(
                    "severity" to Question.Score(
                        "How severe is this issue?",
                        listOf(Question.Score.Level("Cosmetic", "Appearance only"), Question.Score.Level("Fully blocked"))
                    )
                ),
                model
            )
        ).orThrow { error(it) }

        assertThat(criteria, equalTo(listOf<String?>("Cosmetic: Appearance only", "Fully blocked")))
    }

    @Test
    fun `TypeSafe only decides on text`() {
        val result = decide(
            DecideRequest(
                listOf(
                    Content.Text("Inspect the product in this photo."),
                    Content.Image(Resource.Binary(Base64Blob.encode("not really a png")))
                ),
                mapOf("visible_damage" to Question.YesNo("Does the product have visible damage?")),
                model
            )
        )

        assertThat(result, isA<Failure<*>>())
    }
}
