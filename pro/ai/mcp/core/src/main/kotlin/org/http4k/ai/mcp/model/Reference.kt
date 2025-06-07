package org.http4k.ai.mcp.model

import org.http4k.core.Uri
import se.ansman.kotshi.JsonSerializable
import se.ansman.kotshi.Polymorphic
import se.ansman.kotshi.PolymorphicLabel

/**
 * Spec of a Completion capability.
 */
@JsonSerializable
@Polymorphic("type")
sealed class Reference : CapabilitySpec {
    @JsonSerializable
    @PolymorphicLabel("ref/resource")
    data class ResourceTemplate(val uri: Uri) : Reference() {
        constructor(uri: String) : this(Uri.of(uri))
    }

    @JsonSerializable
    @PolymorphicLabel("ref/prompt")
    data class Prompt(val name: String) : Reference() {
        constructor(name: PromptName) : this(name.value)
    }

    companion object {
        fun of(uri: Uri) = ResourceTemplate(uri)
        fun of(name: PromptName) = Prompt(name.value)
    }
}
