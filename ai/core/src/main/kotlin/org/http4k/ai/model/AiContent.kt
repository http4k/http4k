package org.http4k.ai.model

import org.http4k.ai.model.AiContent.Image.DetailLevel.LOW
import se.ansman.kotshi.JsonSerializable
import se.ansman.kotshi.Polymorphic
import se.ansman.kotshi.PolymorphicLabel

@JsonSerializable
@Polymorphic("type")
sealed class AiContent {

    @JsonSerializable
    @PolymorphicLabel("text")
    data class Text(val text: String) : AiContent()

    @JsonSerializable
    @PolymorphicLabel("image")
    data class Image(val image: Resource, val detail: DetailLevel = LOW) : AiContent() {
        enum class DetailLevel { LOW, HIGH, AUTO }
    }

    @JsonSerializable
    @PolymorphicLabel("audio")
    data class Audio(val resource: Resource) : AiContent()

    @JsonSerializable
    @PolymorphicLabel("video")
    data class Video(val resource: Resource) : AiContent()

    @JsonSerializable
    @PolymorphicLabel("pdf")
    data class PDF(val resource: Resource) : AiContent()

    @JsonSerializable
    @PolymorphicLabel("custom")
    data class Custom(val resource: Resource) : AiContent()
}
