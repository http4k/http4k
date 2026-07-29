/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.stateless.model

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
    data class ResourceTemplate(val uri: McpUri) : Reference()

    @JsonSerializable
    @PolymorphicLabel("ref/prompt")
    data class Prompt(val name: String) : Reference() {
        constructor(name: PromptName) : this(name.value)
    }

    companion object {
        fun of(uri: McpUri) = ResourceTemplate(uri)
        fun of(name: PromptName) = Prompt(name.value)
    }
}
