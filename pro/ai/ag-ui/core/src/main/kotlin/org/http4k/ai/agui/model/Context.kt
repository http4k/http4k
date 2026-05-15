/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.agui.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class Context(
    val description: String,
    val value: String
)
