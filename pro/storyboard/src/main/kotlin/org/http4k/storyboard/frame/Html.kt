/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.storyboard.frame

/** Append a raw HTML fragment as a section. */
fun FrameBuilder.html(content: String) = section(Section(htmlBody(content)))

internal fun htmlBody(content: String): String = content
