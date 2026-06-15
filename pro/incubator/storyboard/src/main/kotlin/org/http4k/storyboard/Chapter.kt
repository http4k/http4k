/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.storyboard

data class Chapter(
    val title: String,
    val frames: List<StoryFrame> = emptyList(),
    val children: List<Chapter> = emptyList()
)
