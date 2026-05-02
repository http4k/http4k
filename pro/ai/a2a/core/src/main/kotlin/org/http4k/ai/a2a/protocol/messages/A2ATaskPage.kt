/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.protocol.messages

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class A2ATaskPage(
    val tasks: List<A2ATask>,
    val nextPageToken: String,
    val totalSize: Int
)
