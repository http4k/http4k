/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.storyboard.extractor

import org.http4k.storyboard.util.StoryboardViewModel

data class HttpExchangeView(
    val kind: String,
    val method: String,
    val url: String,
    val path: String,
    val status: String,
    val statusClass: String,
    val durationMs: String,
    val requestSize: String,
    val responseSize: String
) : StoryboardViewModel
