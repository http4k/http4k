package org.http4k.connect.langchain.image

import dev.forkhandles.result4k.map
import dev.langchain4j.data.image.Image
import dev.langchain4j.model.image.ImageModel
import dev.langchain4j.model.output.Response
import org.http4k.ai.model.ModelName
import org.http4k.connect.openai.OpenAI
import org.http4k.connect.openai.OpenAIModels.DALL_E_2
import org.http4k.connect.openai.Quality
import org.http4k.connect.openai.Style
import org.http4k.connect.openai.User
import org.http4k.connect.openai.action.ImageData
import org.http4k.connect.openai.action.ImageResponseFormat
import org.http4k.connect.openai.action.Size
import org.http4k.connect.openai.generateImage
import org.http4k.connect.orThrow

fun OpenAIImageModel(openAi: OpenAI, options: ImageModelOptions = ImageModelOptions()) = object : ImageModel {
    override fun generate(p0: String) = Response(generate(p0, 1).content().first())

    override fun generate(it: String, n: Int) = with(options) {
        openAi.generateImage(it, size, imageResponseFormat, n, user)
            .map { Response(it.data.map { it.toHttp4k() }) }
            .orThrow()
    }
}

private fun ImageData.toHttp4k() = Image.builder().apply {
    b64_json?.also { base64Data(it.value) }
    url?.also { it.toString() }
}
    .build()

data class ImageModelOptions(
    val size: Size = Size.`1024x1024`,
    val model: ModelName = DALL_E_2,
    val imageResponseFormat: ImageResponseFormat = ImageResponseFormat.b64_json,
    val user: User? = null
)
