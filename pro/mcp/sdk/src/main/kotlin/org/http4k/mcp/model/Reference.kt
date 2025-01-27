package org.http4k.mcp.model

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
    data class Resource(val uri: Uri) : Reference()

    @JsonSerializable
    @PolymorphicLabel("ref/prompt")
    data class Prompt(val name: String) : Reference()
}
