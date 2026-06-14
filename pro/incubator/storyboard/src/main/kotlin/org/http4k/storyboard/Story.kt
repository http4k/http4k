/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.storyboard

data class Story(
    val title: String,
    val frames: List<StoryFrame>,
    val className: String? = null,
    val outcome: Outcome? = null,
    val durationMs: Long? = null
) {
    enum class Outcome { Passed, Failed, Aborted }
}
