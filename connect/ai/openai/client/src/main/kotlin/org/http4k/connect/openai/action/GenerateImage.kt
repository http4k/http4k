package org.http4k.connect.openai.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.NonNullAutoMarshalledAction
import org.http4k.connect.kClass
import org.http4k.connect.model.Base64Blob
import org.http4k.connect.model.Timestamp
import org.http4k.connect.openai.OpenAIAction
import org.http4k.connect.openai.OpenAIMoshi
import org.http4k.connect.openai.Quality
import org.http4k.connect.openai.Style
import org.http4k.connect.openai.User
import org.http4k.connect.openai.action.ImageResponseFormat.url
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.core.with
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
@JsonSerializable
data class GenerateImage(
    val prompt: String,
    val size: Size = Size.`1024x1024`,
    val response_format: ImageResponseFormat = url,
    val n: Int = 1,
    val quality: Quality = Quality.standard,
    val style: Style = Style.vivid,
    val user: User? = null,

    ) : NonNullAutoMarshalledAction<GeneratedImage>(kClass(), OpenAIMoshi), OpenAIAction<GeneratedImage> {

    constructor(prompt: String, size: Size) : this(prompt, size, url, 1)

    override fun toRequest() = Request(POST, "/v1/images/generations")
        .with(OpenAIMoshi.autoBody<GenerateImage>().toLens() of this)
}

enum class ImageResponseFormat {
    url, b64_json
}

enum class Size {
    `256x256`, `512x512`, `1024x1024`
}

@JsonSerializable
data class ImageData(
    val url: Uri? = null,
    val b64_json: Base64Blob? = null
)

@JsonSerializable
data class GeneratedImage(
    val created: Timestamp,
    val `data`: List<ImageData>
)
