package org.http4k.ai.llm.image

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.startsWith
import dev.forkhandles.result4k.valueOrNull
import org.http4k.ai.llm.model.Resource
import org.http4k.ai.model.ModelName
import org.http4k.ai.model.UserPrompt
import org.junit.jupiter.api.Test

interface ImageGenerationContract {

    val imageGeneration: ImageGeneration
    val model: ModelName

    @Test
    fun `can generate an image`() {
        val response = imageGeneration(ImageRequest(model, UserPrompt.of("a nice doggy"), ImageResponseFormat.url)).valueOrNull()!!

        val actual = response.resources.first() as Resource.Ref
        assertThat(actual.uri.toString(), startsWith("http"))
    }
}
