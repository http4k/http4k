/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.storyboard

import org.http4k.core.HttpHandler
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.storyboard.render.flatten

internal fun recordStory(
    handler: HttpHandler = { Response(OK) },
    block: Storyboard.(StoryboardWebDriver) -> Unit
): Story = storyboard("test") {
    block(StoryboardWebDriver(handler, this))
}

internal fun recordFrames(
    handler: HttpHandler = { Response(OK) },
    block: Storyboard.(StoryboardWebDriver) -> Unit
): List<StoryFrame> = recordStory(handler, block).flatten().map { it.frame }.dropHttpFrames()

/** Strips the Detail-level HTTP-exchange frames so a test can focus on its own captures. */
internal fun List<StoryFrame>.dropHttpFrames(): List<StoryFrame> =
    filterNot { it.title.startsWith("CLIENT ") || it.title.startsWith("SERVER ") }
