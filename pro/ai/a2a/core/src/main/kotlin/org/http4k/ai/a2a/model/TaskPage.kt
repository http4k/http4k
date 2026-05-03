/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class TaskPage(
    val tasks: List<Task>,
    val nextPageToken: PageToken = PageToken.END,
    val pageSize: Int = 0,
    val totalSize: Int
)
