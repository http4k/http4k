/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.storyboard

/**
 * Turns a finished [Story] into a presentable HTML document.
 */
fun interface StoryRenderer {
    fun render(story: Story): String
}
