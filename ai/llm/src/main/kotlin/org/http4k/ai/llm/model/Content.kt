package org.http4k.ai.llm.model

import org.http4k.ai.llm.model.Content.Image.DetailLevel.LOW
import se.ansman.kotshi.JsonSerializable
import se.ansman.kotshi.Polymorphic
import se.ansman.kotshi.PolymorphicLabel

@JsonSerializable
@Polymorphic("type")
sealed class Content {

    @JsonSerializable
    @PolymorphicLabel("text")
    data class Text(val text: String) : Content()

    @JsonSerializable
    @PolymorphicLabel("image")
    data class Image(val image: Resource, val detail: DetailLevel = LOW) : Content() {
        enum class DetailLevel { LOW, HIGH, AUTO }
    }

    @JsonSerializable
    @PolymorphicLabel("audio")
    data class Audio(val resource: Resource) : Content()

    @JsonSerializable
    @PolymorphicLabel("video")
    data class Video(val resource: Resource) : Content()

    @JsonSerializable
    @PolymorphicLabel("pdf")
    data class PDF(val resource: Resource) : Content()

    @JsonSerializable
    @PolymorphicLabel("custom")
    data class Custom(val resource: Resource) : Content()
}
